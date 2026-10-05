package com.mv.bookingservice.domain.model.booking.aggregate;

import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import com.mv.bookingservice.domain.model.booking.exception.InvalidBookingStateException;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private UUID id;
    private String bookingCode;
    private UUID userId;
    private UUID showtimeId;
    private String movieTitle;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private UUID holdId;
    private String idempotencyKey;
    private String requestHash;
    private String currency;
    private UUID paidPaymentId;
    private Long version;
    private Instant updatedAt;

    @Builder.Default
    private List<BookingSeat> seats = new ArrayList<>();

    public static Booking create(
            UUID userId,
            UUID showtimeId,
            String movieTitle,
            String cinemaName,
            String screenName,
            Instant startTime,
            UUID holdId,
            String idempotencyKey,
            String requestHash,
            String currency,
            Instant expiresAt,
            BigDecimal discountAmount,
            List<BookingSeat> seats
    ) {
        if (userId == null) {
            throw new BookingValidationException("userId must not be null");
        }
        if (showtimeId == null) {
            throw new BookingValidationException("showtimeId must not be null");
        }
        if (movieTitle == null || movieTitle.isBlank()) {
            throw new BookingValidationException("movieTitle must not be blank");
        }
        if (cinemaName == null || cinemaName.isBlank()) {
            throw new BookingValidationException("cinemaName must not be blank");
        }
        if (screenName == null || screenName.isBlank()) {
            throw new BookingValidationException("screenName must not be blank");
        }
        if (startTime == null) {
            throw new BookingValidationException("startTime must not be null");
        }
        if (holdId == null) {
            throw new BookingValidationException("holdId must not be null");
        }
        if (seats == null || seats.isEmpty()) {
            throw new BookingValidationException("Booking must contain at least one seat");
        }

        UUID bookingId = UUID.randomUUID();
        String bookingCode = generateBookingCode();
        Instant now = Instant.now();

        BigDecimal actualDiscount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        if (actualDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BookingValidationException("discountAmount cannot be negative");
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        Set<UUID> showtimeSeatIds = new HashSet<>();
        for (BookingSeat seat : seats) {
            if (seat == null) {
                throw new BookingValidationException("Booking seat must not be null");
            }
            if (!showtimeSeatIds.add(seat.getShowtimeSeatId())) {
                throw new BookingValidationException("Duplicate showtimeSeatId in booking: " + seat.getShowtimeSeatId());
            }
            seat.assignToBooking(bookingId);
            calculatedSubtotal = calculatedSubtotal.add(seat.getUnitPrice());
        }

        BigDecimal calculatedTotal = calculatedSubtotal.subtract(actualDiscount);
        if (calculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            calculatedTotal = BigDecimal.ZERO;
        }

        return Booking.builder()
                .id(bookingId)
                .bookingCode(bookingCode)
                .userId(userId)
                .showtimeId(showtimeId)
                .movieTitle(movieTitle)
                .cinemaName(cinemaName)
                .screenName(screenName)
                .startTime(startTime)
                .subtotal(calculatedSubtotal)
                .discountAmount(actualDiscount)
                .totalAmount(calculatedTotal)
                .status(BookingStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .holdId(holdId)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .currency(currency != null ? currency : "VND")
                .version(0L)
                .updatedAt(now)
                .seats(seats)
                .build();
    }

    public void markConfirming(UUID paymentId) {
        if (isExpired()) {
            throw new InvalidBookingStateException("Booking has already expired");
        }
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(status, "markConfirming");
        }
        this.status = BookingStatus.CONFIRMING;
        this.paidPaymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public void confirm(UUID paymentId) {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMING) {
            throw new InvalidBookingStateException(status, "confirm");
        }
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        if (paymentId != null) {
            this.paidPaymentId = paymentId;
        }
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(status, "cancel");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
        this.seats.forEach(BookingSeat::cancel);
    }

    public void expire() {
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(status, "expire");
        }
        this.status = BookingStatus.EXPIRED;
        this.updatedAt = Instant.now();
        this.seats.forEach(BookingSeat::expire);
    }

    public void requestRefund() {
        if (status != BookingStatus.CONFIRMING && status != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(status, "requestRefund");
        }
        this.status = BookingStatus.REFUND_PENDING;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        if (status != BookingStatus.REFUND_PENDING && status != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(status, "markRefunded");
        }
        this.status = BookingStatus.REFUNDED;
        this.updatedAt = Instant.now();
        this.seats.forEach(BookingSeat::refund);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    private static String generateBookingCode() {
        return "BK" + (System.currentTimeMillis() % 100000000) + (int)(Math.random() * 900 + 100);
    }
}
