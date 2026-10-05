package com.mv.paymentservice.application.service;

import com.mv.paymentservice.application.port.in.GetPaymentUseCase;
import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPaymentService implements GetPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;

    @Override
    public Payment getPaymentById(GetPaymentQuery query) {
        return loadPaymentPort.findById(query.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(query.getPaymentId()));
    }

    @Override
    public List<Payment> getPaymentsByBookingId(UUID bookingId) {
        return loadPaymentPort.findByBookingId(bookingId);
    }
}
