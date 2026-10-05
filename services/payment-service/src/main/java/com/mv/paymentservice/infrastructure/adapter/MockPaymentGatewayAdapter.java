package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public String generatePaymentUrl(Payment payment) {
        String provider = payment.getProvider() != null ? payment.getProvider().toLowerCase() : "mock";
        return String.format("https://gateway.example.com/%s/pay?paymentId=%s&amount=%s",
                provider,
                payment.getId(),
                payment.getAmount()
        );
    }
}
