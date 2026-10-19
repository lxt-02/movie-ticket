package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.application.dto.PaymentDefaults;
import com.mv.paymentservice.application.dto.PaymentLink;
import com.mv.paymentservice.application.dto.PaymentLinkRequest;
import com.mv.paymentservice.application.event.PaymentEvent;
import com.mv.paymentservice.application.event.PaymentEventFactory;
import com.mv.paymentservice.application.port.in.CreatePaymentUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentEventOutboxPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Handles CREATE_PAYMENT from booking-service. booking-service sends one idempotency key per payment attempt
 * ({@code payment:create:PAYOS:{bookingId}:{attempt}}), so a new key means "pay again".
 * <ol>
 *     <li>Same idempotency key seen before: re-publish the current state of that payment.</li>
 *     <li>Booking already paid: re-publish PAYMENT_SUCCEEDED, never open a second link.</li>
 *     <li>Booking has an open link from an earlier attempt: cancel it on PayOS first. If PayOS says it was paid
 *     meanwhile, it becomes SUCCESS and no new link is made; if it cannot be closed yet, no new link either.</li>
 *     <li>Create a PayOS link, store the payment and publish PAYMENT_LINK_CREATED.</li>
 * </ol>
 * Any exception is turned into PAYMENT_CREATE_FAILED by the Kafka listener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    static final String IDEMPOTENCY_PREFIX = "payment:create:" + Payment.PROVIDER_PAYOS + ":";
    static final String REPLACED_REASON = "Replaced by a new payment attempt";

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentEventOutboxPort outboxPort;
    private final PaymentSettlement settlement;
    private final PaymentDefaults paymentDefaults;
    private final Clock clock;

    @Override
    @Transactional
    public Payment createPayment(CreatePaymentCommand command) {
        validate(command);
        Instant now = clock.instant();
        String idempotencyKey = resolveIdempotencyKey(command);

        Optional<Payment> sameKey = loadPaymentPort.findByIdempotencyKey(idempotencyKey);
        if (sameKey.isPresent()) {
            Payment existing = sameKey.get();
            log.info("CREATE_PAYMENT replay for booking {} (key {}), payment {} is {}",
                    command.getBookingId(), idempotencyKey, existing.getId(), existing.getStatus());
            PaymentEvent event = PaymentEventFactory.currentState(existing, now);
            if (event != null) {
                outboxPort.append(event);
            }
            return existing;
        }

        List<Payment> bookingPayments = loadPaymentPort.findByBookingIdForUpdate(command.getBookingId());
        Optional<Payment> succeeded = bookingPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();
        if (succeeded.isPresent()) {
            log.info("Booking {} is already paid by payment {}, no new link", command.getBookingId(), succeeded.get().getId());
            outboxPort.append(PaymentEventFactory.statusChanged(succeeded.get(), now));
            return succeeded.get();
        }

        for (Payment open : bookingPayments) {
            if (open.getStatus().isTerminal()) {
                continue;
            }
            GatewaySyncOutcome outcome = settlement.cancelAndClose(open, PaymentStatus.CANCELLED, REPLACED_REASON, now);
            if (outcome == GatewaySyncOutcome.PAID) {
                log.info("Earlier payment {} of booking {} was paid meanwhile, no new link", open.getId(), command.getBookingId());
                return open;
            }
            if (outcome == GatewaySyncOutcome.STILL_OPEN) {
                // Money may still be on its way to the old link: a second link could make the buyer pay twice.
                log.warn("Earlier payment {} of booking {} is still open on PayOS, not creating a new link",
                        open.getId(), command.getBookingId());
                outboxPort.append(PaymentEventFactory.linkCreated(open, now));
                return open;
            }
        }

        Instant expiresAt = resolveExpiry(command, now);
        Payment payment = Payment.createPending(
                command.getBookingId(),
                command.getUserId(),
                command.getAmount(),
                command.getCurrency(),
                idempotencyKey,
                expiresAt,
                now
        );

        PaymentLink link = paymentGatewayPort.createPaymentLink(new PaymentLinkRequest(
                payment.getPayosOrderCode(),
                payment.amountAsLong(),
                transferDescription(payment.getPayosOrderCode()),
                expiresAt
        ));
        payment.attachPaymentLink(link.paymentLinkId(), link.checkoutUrl(), link.qrCode(), now);

        Payment saved = savePaymentPort.save(payment);
        outboxPort.append(PaymentEventFactory.linkCreated(saved, now));
        log.info("Created PayOS payment {} (orderCode {}) for booking {}",
                saved.getId(), saved.getPayosOrderCode(), saved.getBookingId());
        return saved;
    }

    private void validate(CreatePaymentCommand command) {
        if (command == null) {
            throw new InvalidPaymentException("CREATE_PAYMENT command must not be null");
        }
        if (command.getBookingId() == null) {
            throw new InvalidPaymentException("bookingId is required");
        }
        if (command.getUserId() == null) {
            throw new InvalidPaymentException("userId is required");
        }
        String provider = command.getProvider();
        if (provider != null && !provider.isBlank() && !Payment.PROVIDER_PAYOS.equalsIgnoreCase(provider.trim())) {
            throw new InvalidPaymentException("Unsupported payment provider: " + provider);
        }
        Payment.validateVndAmount(command.getAmount());
    }

    private String resolveIdempotencyKey(CreatePaymentCommand command) {
        String key = command.getIdempotencyKey();
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        return IDEMPOTENCY_PREFIX + command.getBookingId();
    }

    private Instant resolveExpiry(CreatePaymentCommand command, Instant now) {
        if (command.getExpiredAt() != null) {
            if (!command.getExpiredAt().isAfter(now)) {
                throw new InvalidPaymentException("expiredAt must be in the future: " + command.getExpiredAt());
            }
            return command.getExpiredAt();
        }
        return now.plus(paymentDefaults.paymentExpiration());
    }

    /** "SP" + last 7 digits of the order code: short enough for PayOS's 25-character limit. */
    static String transferDescription(long orderCode) {
        String digits = Long.toString(orderCode);
        return "SP" + digits.substring(Math.max(0, digits.length() - 7));
    }
}
