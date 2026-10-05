package com.mv.bookingservice.infrastructure.persistence.entity;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 50)
    private String bookingCode;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "showtime_id", nullable = false)
    private UUID showtimeId;

    @Column(name = "movie_title", nullable = false, length = 255)
    private String movieTitle;

    @Column(name = "cinema_name", nullable = false, length = 255)
    private String cinemaName;

    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "hold_id", unique = true)
    private UUID holdId;

    @Column(name = "idempotency_key", length = 150)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "paid_payment_id")
    private UUID paidPaymentId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeatEntity> seats = new ArrayList<>();

    public void addSeat(BookingSeatEntity seat) {
        seats.add(seat);
        seat.setBooking(this);
    }
}
