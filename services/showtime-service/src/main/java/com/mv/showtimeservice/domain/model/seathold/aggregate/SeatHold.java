package com.mv.showtimeservice.domain.model.seathold.aggregate;

import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.seathold.exception.InvalidSeatHoldStateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHold {
    private UUID id;
    private UUID bookingId;
    private UUID showtimeId;
    private String idempotencyKey;
    private String requestHash;
    private SeatHoldStatus status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<SeatHoldItem> items = new ArrayList<>();

    public static SeatHold create(
            UUID bookingId,
            UUID showtimeId,
            String idempotencyKey,
            String requestHash,
            Instant expiresAt,
            List<SeatHoldItem> items
    ) {
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId must not be null");
        }
        if (showtimeId == null) {
            throw new IllegalArgumentException("showtimeId must not be null");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (requestHash == null || requestHash.isBlank()) {
            throw new IllegalArgumentException("requestHash must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        UUID holdId = UUID.randomUUID();
        Instant now = Instant.now();

        if (items != null) {
            items.forEach(item -> {
                item.setHoldId(holdId);
                item.setShowtimeId(showtimeId);
            });
        }

        return SeatHold.builder()
                .id(holdId)
                .bookingId(bookingId)
                .showtimeId(showtimeId)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(SeatHoldStatus.HELD)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .items(items != null ? items : new ArrayList<>())
                .build();
    }

    public void confirm() {
        if (status == SeatHoldStatus.CONFIRMED) {
            return; // Idempotent confirm
        }
        if (status != SeatHoldStatus.HELD) {
            throw new InvalidSeatHoldStateException(status, "confirm");
        }
        this.status = SeatHoldStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void release() {
        if (status != SeatHoldStatus.HELD) {
            throw new InvalidSeatHoldStateException(status, "release");
        }
        this.status = SeatHoldStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        if (status != SeatHoldStatus.HELD) {
            throw new InvalidSeatHoldStateException(status, "expire");
        }
        this.status = SeatHoldStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
