package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

public interface SavePaymentPort {
    Payment save(Payment payment);
}
