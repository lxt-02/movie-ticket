package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.domain.model.refund.aggregate.Refund;
import com.mv.paymentservice.domain.repository.RefundRepository;
import com.mv.paymentservice.infrastructure.persistence.entity.RefundEntity;
import com.mv.paymentservice.infrastructure.persistence.mapper.RefundPersistenceMapper;
import com.mv.paymentservice.infrastructure.persistence.repository.RefundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepository {

    private final RefundJpaRepository refundJpaRepository;
    private final RefundPersistenceMapper refundPersistenceMapper;

    @Override
    public Refund save(Refund refund) {
        RefundEntity entity = refundPersistenceMapper.toEntity(refund);
        RefundEntity saved = refundJpaRepository.save(entity);
        return refundPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return refundJpaRepository.findById(id)
                .map(refundPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey) {
        return refundJpaRepository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .map(refundPersistenceMapper::toDomain);
    }

    @Override
    public List<Refund> findByPaymentId(UUID paymentId) {
        return refundJpaRepository.findByPaymentId(paymentId).stream()
                .map(refundPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
