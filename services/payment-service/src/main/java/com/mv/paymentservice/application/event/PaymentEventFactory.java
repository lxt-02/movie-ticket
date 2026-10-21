package com.mv.paymentservice.application.event;

import com.mv.paymentservice.application.command.CreatePaymentCommand;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the payloads of the payment-events contract (see rule/payment/payment_service_payos.md, section 5.2).
 */
public final class PaymentEventFactory {

    public static final String REASON_PROVIDER_ERROR = "PAYMENT_PROVIDER_ERROR";
    public static final String REASON_INVALID_REQUEST = "INVALID_PAYMENT_REQUEST";

    private PaymentEventFactory() {
    }

    public static PaymentEvent linkCreated(Payment payment, Instant now) {
        Map<String, Object> payload = basePayload(payment);
        payload.put("checkoutUrl", payment.getCheckoutUrl());
        payload.put("qrCode", payment.getQrCode());
        payload.put("expiredAt", payment.getExpiredAt());
        return event(payment.getBookingId(), PaymentEventType.PAYMENT_LINK_CREATED, now, payload);
    }

    public static PaymentEvent createFailed(CreatePaymentCommand command, String reason, String message, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", command.getBookingId());
        payload.put("userId", command.getUserId());
        payload.put("provider", command.getProvider() == null || command.getProvider().isBlank()
                ? Payment.PROVIDER_PAYOS : command.getProvider());
        payload.put("amount", command.getAmount());
        payload.put("currency", command.getCurrency());
        payload.put("reason", reason);
        payload.put("message", message);
        return event(command.getBookingId(), PaymentEventType.PAYMENT_CREATE_FAILED, now, payload);
    }

    /** PAYMENT_SUCCEEDED / PAYMENT_FAILED / PAYMENT_EXPIRED / PAYMENT_CANCELLED, chosen from the payment status. */
    public static PaymentEvent statusChanged(Payment payment, Instant now) {
        PaymentEventType type = switch (payment.getStatus()) {
            case SUCCESS -> PaymentEventType.PAYMENT_SUCCEEDED;
            case EXPIRED -> PaymentEventType.PAYMENT_EXPIRED;
            case CANCELLED -> PaymentEventType.PAYMENT_CANCELLED;
            case FAILED -> PaymentEventType.PAYMENT_FAILED;
            default -> throw new IllegalStateException("No status event for payment status " + payment.getStatus());
        };
        Map<String, Object> payload = basePayload(payment);
        payload.put("gatewayTransactionId", payment.getGatewayTransactionId());
        payload.put("failureReason", payment.getFailureReason());
        payload.put("paidAt", payment.getPaidAt());
        payload.put("expiredAt", payment.getExpiredAt());
        return event(payment.getBookingId(), type, now, payload);
    }

    /**
     * Event describing the current state of an existing payment, re-sent when booking repeats a command.
     */
    public static PaymentEvent currentState(Payment payment, Instant now) {
        PaymentStatus status = payment.getStatus();
        return switch (status) {
            case PENDING, PROCESSING -> linkCreated(payment, now);
            case SUCCESS, FAILED, EXPIRED, CANCELLED -> statusChanged(payment, now);
        };
    }

    private static Map<String, Object> basePayload(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("bookingId", payment.getBookingId());
        payload.put("userId", payment.getUserId());
        payload.put("provider", payment.getGatewayProvider());
        payload.put("paymentStatus", payment.getStatus());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        return payload;
    }

    private static PaymentEvent event(UUID bookingId, PaymentEventType type, Instant now, Map<String, Object> payload) {
        return new PaymentEvent(UUID.randomUUID(), bookingId, type, now, payload);
    }
}
