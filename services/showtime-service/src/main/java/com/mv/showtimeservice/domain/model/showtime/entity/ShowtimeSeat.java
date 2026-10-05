package com.mv.showtimeservice.domain.model.showtime.entity;

import com.mv.showtimeservice.domain.model.seathold.exception.SeatUnavailableException;
import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSeat {
    private UUID id;
    private UUID showtimeId;
    private UUID seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
    private ShowtimeSeatStatus status;
    private Instant lockedUntil;
    private UUID holdId;

    public void hold(UUID holdId, Instant lockedUntil) {
        if (!isAvailable()) {
            throw new SeatUnavailableException("Seat " + seatLabel + " is not available for hold (Current status: " + status + ")");
        }
        this.status = ShowtimeSeatStatus.HELD;
        this.holdId = holdId;
        this.lockedUntil = lockedUntil;
    }

    public void book() {
        this.status = ShowtimeSeatStatus.BOOKED;
        this.lockedUntil = null;
    }

    public void release() {
        this.status = ShowtimeSeatStatus.AVAILABLE;
        this.holdId = null;
        this.lockedUntil = null;
    }

    public boolean isAvailable() {
        if (status == ShowtimeSeatStatus.AVAILABLE) {
            return true;
        }
        // If seat was HELD but the lock has expired, it can be reclaimed once old hold is handled
        return status == ShowtimeSeatStatus.HELD && lockedUntil != null && Instant.now().isAfter(lockedUntil);
    }
}
