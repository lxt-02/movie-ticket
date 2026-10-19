package com.mv.paymentservice.domain.model.payment.exception;

/**
 * PayOS webhook rejected: bad signature, malformed body or data that does not match the stored payment.
 * Mapped to HTTP 400.
 */
public class InvalidWebhookException extends DomainException {
    public InvalidWebhookException(String message) {
        super(message);
    }
}
