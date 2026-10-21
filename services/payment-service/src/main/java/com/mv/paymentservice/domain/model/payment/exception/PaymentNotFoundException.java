package com.mv.paymentservice.domain.model.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends DomainException {
    public PaymentNotFoundException(UUID paymentId) {
        super("Không tìm thấy thanh toán có id: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
