package com.mv.showtimeservice.domain.model.showtime.aggregate;

import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import com.mv.showtimeservice.domain.model.showtime.exception.InvalidShowtimeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Showtime {
    private UUID id;
    private UUID movieId;
    private UUID screenId;
    private String movieTitle;
    private UUID cinemaId;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private Instant endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Instant createdAt;

    public boolean isSelling() {
        return (status == ShowtimeStatus.SCHEDULED || status == ShowtimeStatus.SELLING)
                && startTime.isAfter(Instant.now());
    }

    public boolean isCancelled() {
        return status == ShowtimeStatus.CANCELLED;
    }

    /** Start / price can only change for a SCHEDULED or SELLING showtime that has not started yet. */
    public void assertEditable(Instant now) {
        if (status == ShowtimeStatus.CANCELLED || status == ShowtimeStatus.ENDED) {
            throw new InvalidShowtimeException("Suất chiếu " + id + " đang ở trạng thái " + status + " nên không thể thay đổi");
        }
        if (!startTime.isAfter(now)) {
            throw new InvalidShowtimeException("Suất chiếu " + id + " đã bắt đầu chiếu");
        }
    }

    /** Moves the showtime; the length (end - start) stays the same. */
    public void reschedule(Instant newStart, Instant now) {
        assertEditable(now);
        if (newStart == null || !newStart.isAfter(now)) {
            throw new InvalidShowtimeException("Giờ chiếu phải ở tương lai");
        }
        Duration length = Duration.between(startTime, endTime);
        this.startTime = newStart;
        this.endTime = newStart.plus(length);
    }

    public void changeBasePrice(BigDecimal newBasePrice, Instant now) {
        assertEditable(now);
        if (newBasePrice == null || newBasePrice.signum() < 0) {
            throw new InvalidShowtimeException("Giá vé không được âm");
        }
        this.basePrice = newBasePrice;
    }

    /**
     * New seat price after a base price change, keeping the seat type multiplier (VIP 1.5x stays 1.5x).
     * When the old base was 0 the multiplier is unknown, so the seat gets the new base price.
     */
    public static BigDecimal repriceSeat(BigDecimal oldSeatPrice, BigDecimal oldBase, BigDecimal newBase) {
        if (oldBase == null || oldBase.signum() == 0 || oldSeatPrice == null) {
            return newBase.setScale(2, RoundingMode.HALF_UP);
        }
        return oldSeatPrice.multiply(newBase).divide(oldBase, 2, RoundingMode.HALF_UP);
    }

    /** CANCELLED is final. Already cancelled: no change. An ENDED showtime cannot be cancelled. */
    public void cancel() {
        if (status == ShowtimeStatus.CANCELLED) {
            return;
        }
        if (status == ShowtimeStatus.ENDED) {
            throw new InvalidShowtimeException("Suất chiếu " + id + " đã chiếu xong nên không thể huỷ");
        }
        this.status = ShowtimeStatus.CANCELLED;
    }
}
