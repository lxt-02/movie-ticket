package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.port.out.LoadPaymentPort;
import com.mv.paymentservice.application.port.out.SavePaymentPort;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.mv.paymentservice.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.mv.paymentservice.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements LoadPaymentPort, SavePaymentPort {

    private static final List<PaymentStatus> ACTIVE_STATUSES = List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING);

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity saved = paymentJpaRepository.save(mapper.toEntity(payment));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentJpaRepository.findFirstByIdempotencyKeyOrderByCreatedAtDesc(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findActiveByBookingId(UUID bookingId) {
        return paymentJpaRepository
                .findFirstByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(bookingId, ACTIVE_STATUSES)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findLatestByBookingId(UUID bookingId) {
        return paymentJpaRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByBookingId(UUID bookingId) {
        return paymentJpaRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByBookingIdForUpdate(UUID bookingId) {
        return paymentJpaRepository.findByBookingIdForUpdate(bookingId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Payment> findByIdForUpdate(UUID id) {
        return paymentJpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByPayosOrderCodeForUpdate(long orderCode) {
        return paymentJpaRepository.findByPayosOrderCodeForUpdate(orderCode).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findDuePendingForUpdate(Instant now, int limit) {
        return paymentJpaRepository.findDuePendingForUpdate(now, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findOpenForReconcileForUpdate(Instant createdBefore, Instant now, int limit) {
        return paymentJpaRepository.findOpenForReconcileForUpdate(createdBefore, now, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
