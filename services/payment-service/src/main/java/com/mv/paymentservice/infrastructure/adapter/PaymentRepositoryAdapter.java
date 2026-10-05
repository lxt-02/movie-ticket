package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.repository.PaymentRepository;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.mv.paymentservice.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.mv.paymentservice.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository, LoadPaymentPort, SavePaymentPort {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = paymentPersistenceMapper.toEntity(payment);
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return paymentPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey) {
        return paymentJpaRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public List<Payment> findByBookingId(UUID bookingId) {
        return paymentJpaRepository.findByBookingId(bookingId).stream()
                .map(paymentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
