package com.mv.cinemaservice.domain.model.cinema.aggregate;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import com.mv.cinemaservice.domain.model.cinema.exception.InvalidCinemaException;
import com.mv.cinemaservice.domain.model.cinema.valueobject.CinemaDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cinema {
    private UUID id;
    private String name;
    private String brand;
    private String address;
    private String city;
    private String district;
    private String phone;
    private CinemaStatus status;
    private Instant createdAt;

    public boolean isActive() {
        return status == CinemaStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == CinemaStatus.CLOSED;
    }

    /** New cinema: ACTIVE by default, or INACTIVE (prepared but not open yet). Never CLOSED. */
    public static Cinema create(CinemaDetails details, CinemaStatus initialStatus, Instant now) {
        CinemaStatus status = initialStatus == null ? CinemaStatus.ACTIVE : initialStatus;
        if (status == CinemaStatus.CLOSED) {
            throw new InvalidCinemaException("Rạp mới không được ở trạng thái CLOSED");
        }
        Cinema cinema = Cinema.builder()
                .id(UUID.randomUUID())
                .status(status)
                .createdAt(now)
                .build();
        cinema.apply(details);
        return cinema;
    }

    /** A closed cinema is read-only. */
    public void update(CinemaDetails details) {
        if (isClosed()) {
            throw new InvalidCinemaException("Rạp đã đóng cửa không được sửa");
        }
        apply(details);
    }

    /**
     * ACTIVE <-> INACTIVE (temporarily not selling), ACTIVE / INACTIVE -> CLOSED (permanent). Same status: no change.
     * Only ACTIVE cinemas accept new showtimes (checked by showtime-service).
     */
    public void changeStatus(CinemaStatus target) {
        if (target == null) {
            throw new InvalidCinemaException("Trạng thái là bắt buộc");
        }
        if (target == status) {
            return;
        }
        if (isClosed()) {
            throw new InvalidCinemaException("Rạp đã đóng cửa không thể mở lại");
        }
        this.status = target;
    }

    /** "Delete" = close (soft delete): screens, showtimes and bookings keep referring to it. Idempotent. */
    public void close() {
        changeStatus(CinemaStatus.CLOSED);
    }

    private void apply(CinemaDetails details) {
        if (details == null) {
            throw new InvalidCinemaException("Thiếu thông tin rạp");
        }
        this.name = details.name();
        this.brand = details.brand();
        this.address = details.address();
        this.city = details.city();
        this.district = details.district();
        this.phone = details.phone();
    }
}
