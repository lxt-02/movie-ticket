package com.mv.paymentservice.application.port.in;

import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;

import java.util.List;
import java.util.UUID;

public interface GetPaymentUseCase {
    Payment getPaymentById(GetPaymentQuery query);
    List<Payment> getPaymentsByBookingId(UUID bookingId);
}
