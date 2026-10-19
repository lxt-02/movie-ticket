package com.mv.paymentservice.infrastructure.persistence.repository;

import com.mv.paymentservice.domain.model.payment.enums.PaymentStatus;
import com.mv.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findFirstByIdempotencyKeyOrderByCreatedAtDesc(String idempotencyKey);

    /** Latest usable payment of a booking (to reuse its link). */
    Optional<PaymentEntity> findFirstByBookingIdAndPaymentStatusInOrderByCreatedAtDesc(
            UUID bookingId, Collection<PaymentStatus> statuses);

    Optional<PaymentEntity> findFirstByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    List<PaymentEntity> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.id = :id")
    Optional<PaymentEntity> findByIdForUpdate(@Param("id") UUID id);

    /** All payments of a booking, newest first, locked. Used for retries, cancel and sync. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.bookingId = :bookingId order by p.createdAt desc")
    List<PaymentEntity> findByBookingIdForUpdate(@Param("bookingId") UUID bookingId);

    /** Webhook: SELECT ... WHERE payos_order_code = ? FOR UPDATE */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.payosOrderCode = :orderCode")
    Optional<PaymentEntity> findByPayosOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    /** Expiration scheduler: overdue PENDING payments, skipping rows locked by another instance. */
    @Query(value = """
            SELECT * FROM payments
            WHERE payment_status = 'PENDING'
              AND expired_at IS NOT NULL
              AND expired_at <= :now
            ORDER BY expired_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentEntity> findDuePendingForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    /** Reconcile job: PENDING, old enough, not overdue (overdue ones belong to the expiration job). */
    @Query(value = """
            SELECT * FROM payments
            WHERE payment_status = 'PENDING'
              AND created_at <= :createdBefore
              AND (expired_at IS NULL OR expired_at > :now)
            ORDER BY updated_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentEntity> findOpenForReconcileForUpdate(@Param("createdBefore") Instant createdBefore,
                                                      @Param("now") Instant now,
                                                      @Param("limit") int limit);
}
