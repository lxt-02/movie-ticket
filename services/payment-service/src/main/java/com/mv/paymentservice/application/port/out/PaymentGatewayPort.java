package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.application.dto.PaymentLink;
import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkRequest;
import com.mv.paymentservice.application.dto.PayosWebhook;

/**
 * Payment gateway (PayOS). Methods throw PaymentGatewayException on gateway errors.
 */
public interface PaymentGatewayPort {

    PaymentLink createPaymentLink(PaymentLinkRequest request);

    void cancelPaymentLink(long orderCode, String reason);

    /** Current status, amounts and transfers of a link. */
    PaymentLinkInfo getPaymentLinkInfo(long orderCode);

    /**
     * Parses the webhook body and verifies its signature with the checksum key.
     *
     * @throws com.mv.paymentservice.domain.model.payment.exception.InvalidWebhookException if invalid
     */
    PayosWebhook verifyWebhook(String rawBody);
}
