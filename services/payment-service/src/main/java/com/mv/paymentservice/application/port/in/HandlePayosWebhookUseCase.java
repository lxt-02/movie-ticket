package com.mv.paymentservice.application.port.in;

public interface HandlePayosWebhookUseCase {
    /**
     * Verifies and applies a PayOS webhook.
     *
     * @throws com.mv.paymentservice.domain.model.payment.exception.InvalidWebhookException on bad signature or mismatched data
     */
    void handle(String rawBody);
}
