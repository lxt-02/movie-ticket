package com.mv.paymentservice.application.exception;

/**
 * Error talking to the payment gateway (PayOS): network failure, non-"00" response code, unexpected body.
 */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
