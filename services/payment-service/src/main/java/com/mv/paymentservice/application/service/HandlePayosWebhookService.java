package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.dto.PayosWebhook;
import com.mv.paymentservice.application.dto.PayosWebhookData;
import com.mv.paymentservice.application.port.in.HandlePayosWebhookUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.domain.model.payment.exception.InvalidWebhookException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * POST /api/v1/payments/payos/webhook.
 * <ol>
 *     <li>Verify the signature with the checksum key (400 if invalid).</li>
 *     <li>Lock the payment by orderCode. Unknown orderCode: log and return 200 so PayOS stops retrying.</li>
 *     <li>Check orderCode, amount, currency (VND) and paymentLinkId against the stored payment (400 if any differs).</li>
 *     <li>Store the webhook once in payment_transactions.</li>
 *     <li>Apply the status change and write the event to the outbox, all in one transaction.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePayosWebhookService implements HandlePayosWebhookUseCase {

    private final PaymentGatewayPort paymentGatewayPort;
    private final LoadPaymentPort loadPaymentPort;
    private final PaymentSettlement settlement;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(String rawBody) {
        PayosWebhook webhook = paymentGatewayPort.verifyWebhook(rawBody);
        PayosWebhookData data = webhook.data();
        if (data == null || data.orderCode() == null) {
            throw new InvalidWebhookException("Webhook has no data.orderCode");
        }

        Optional<Payment> found = loadPaymentPort.findByPayosOrderCodeForUpdate(data.orderCode());
        if (found.isEmpty()) {
            // Also covers the test webhook PayOS sends when the URL is registered on the dashboard.
            log.warn("PayOS webhook for unknown orderCode {} ignored", data.orderCode());
            return;
        }
        Payment payment = found.get();
        verifyMatchesPayment(payment, data);

        Instant now = clock.instant();
        boolean paid = webhook.isPaid();
        String reference = data.reference() != null && !data.reference().isBlank()
                ? data.reference()
                : "NO-REF-" + webhook.code() + "-" + data.code();
        boolean inserted = settlement.recordTransaction(payment, reference, data.paymentLinkId(), paid,
                data.amount(), webhook.rawBody(), now);
        if (!inserted) {
            log.info("Duplicate PayOS webhook for payment {} (reference {})", payment.getId(), reference);
        }

        if (paid) {
            settlement.applyPaid(payment, data.paymentLinkId(), data.transactionTime(), now);
            return;
        }
        String reason = "payOS webhook failed: code=" + webhook.code() + ", desc=" + webhook.desc()
                + ", dataCode=" + data.code() + ", dataDesc=" + data.desc();
        PaymentStatus target = looksExpired(webhook) ? PaymentStatus.EXPIRED : PaymentStatus.FAILED;
        if (!settlement.applyClosed(payment, target, reason, now)) {
            log.info("Non-success webhook ignored for payment {} in status {}", payment.getId(), payment.getStatus());
        }
    }

    private void verifyMatchesPayment(Payment payment, PayosWebhookData data) {
        if (!Objects.equals(payment.getPayosOrderCode(), data.orderCode())) {
            throw new InvalidWebhookException("orderCode mismatch for payment " + payment.getId());
        }
        if (data.amount() == null || data.amount() != payment.amountAsLong()) {
            throw new InvalidWebhookException("Amount mismatch for payment " + payment.getId()
                    + ": expected " + payment.amountAsLong() + ", got " + data.amount());
        }
        if (!Payment.CURRENCY_VND.equalsIgnoreCase(data.currency())) {
            throw new InvalidWebhookException("Currency mismatch for payment " + payment.getId() + ": " + data.currency());
        }
        if (payment.getGatewayTransactionId() == null
                || !payment.getGatewayTransactionId().equals(data.paymentLinkId())) {
            throw new InvalidWebhookException("paymentLinkId mismatch for payment " + payment.getId());
        }
    }

    private boolean looksExpired(PayosWebhook webhook) {
        String text = String.join(" ",
                String.valueOf(webhook.code()), String.valueOf(webhook.desc()),
                String.valueOf(webhook.data().code()), String.valueOf(webhook.data().desc())
        ).toLowerCase(Locale.ROOT);
        return text.contains("expire") || text.contains("het han") || text.contains("hết hạn");
    }
}
