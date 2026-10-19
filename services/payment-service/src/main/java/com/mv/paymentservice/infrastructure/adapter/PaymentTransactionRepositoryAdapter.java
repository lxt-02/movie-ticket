package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.SavePaymentTransactionPort;
import com.mv.paymentservice.domain.model.payment.entity.PaymentTransaction;
import com.mv.paymentservice.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.mv.paymentservice.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentTransactionRepositoryAdapter implements SavePaymentTransactionPort {

    private final PaymentTransactionJpaRepository repository;
    private final PaymentPersistenceMapper mapper;

    /**
     * Called while the payment row is locked (webhook), so check-then-insert cannot race for the same payment.
     * The unique index uk_payment_transactions_gateway_reference is the last line of defence.
     */
    @Override
    public boolean saveIfAbsent(PaymentTransaction transaction) {
        boolean exists = repository.existsByPaymentIdAndTransactionCodeAndGatewayTransactionId(
                transaction.getPaymentId(), transaction.getTransactionCode(), transaction.getGatewayTransactionId());
        if (exists) {
            return false;
        }
        repository.save(mapper.toEntity(transaction));
        return true;
    }
}
