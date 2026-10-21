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
    /** 1 for the first payment link; incremented by each "pay again". */
    private Integer paymentAttempt;
    /**
     * Money was received that this booking cannot use (paid late / twice / wrong amount, seats lost after payment).
     * No refund flow in this system: the flag lists the case for manual handling.
     */
    private boolean needsRefund;
    private String refundNote;

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
        return create(
                UUID.randomUUID(),
                userId,
                showtimeId,
                movieTitle,
                cinemaName,
                screenName,
                startTime,
                holdId,
                idempotencyKey,
                requestHash,
                currency,
                expiresAt,
                discountAmount,
                seats
        );
    }

    public static Booking create(
            UUID bookingId,
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
        if (bookingId == null) {
            throw new BookingValidationException("Mã booking là bắt buộc");
        }
        if (userId == null) {
            throw new BookingValidationException("Người dùng là bắt buộc");
        }
        if (showtimeId == null) {
            throw new BookingValidationException("Suất chiếu là bắt buộc");
        }
        if (movieTitle == null || movieTitle.isBlank()) {
            throw new BookingValidationException("Thiếu tên phim");
        }
        if (cinemaName == null || cinemaName.isBlank()) {
            throw new BookingValidationException("Thiếu tên rạp");
        }
        if (screenName == null || screenName.isBlank()) {
            throw new BookingValidationException("Thiếu tên phòng");
        }
        if (startTime == null) {
            throw new BookingValidationException("Thiếu giờ chiếu");
        }
        if (holdId == null) {
            throw new BookingValidationException("Thiếu lượt giữ ghế");
        }
        if (seats == null || seats.isEmpty()) {
            throw new BookingValidationException("Booking phải có ít nhất một ghế");
        }

        String bookingCode = generateBookingCode();
        Instant now = Instant.now();

        BigDecimal actualDiscount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        if (actualDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BookingValidationException("Tiền giảm giá không được âm");
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        Set<UUID> showtimeSeatIds = new HashSet<>();
        for (BookingSeat seat : seats) {
            if (seat == null) {
                throw new BookingValidationException("Thiếu thông tin ghế");
            }
            if (!showtimeSeatIds.add(seat.getShowtimeSeatId())) {
                throw new BookingValidationException("Ghế bị chọn trùng: " + seat.getShowtimeSeatId());
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
                .paymentAttempt(1)
                .seats(seats)
                .build();
    }

    public int currentPaymentAttempt() {
        return paymentAttempt == null || paymentAttempt < 1 ? 1 : paymentAttempt;
    }

    /**
     * "Pay again": only for a PENDING booking whose seat hold has not expired, at most {@code maxAttempts} links.
     */
    public void startNewPaymentAttempt(int maxAttempts) {
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(status, "retryPayment");
        }
        if (isExpired()) {
            throw new InvalidBookingStateException("Booking đã hết hạn");
        }
        if (currentPaymentAttempt() >= maxAttempts) {
            throw new BookingValidationException("Đã vượt quá số lần thanh toán cho phép (" + maxAttempts + ")");
        }
        this.paymentAttempt = currentPaymentAttempt() + 1;
        this.updatedAt = Instant.now();
    }

    /** True if the money was paid before the seat hold expired (the event may be processed later). */
    public boolean wasPaidInTime(Instant paidAt) {
        return expiresAt == null || paidAt == null || !paidAt.isAfter(expiresAt);
    }

    /**
     * Payment received in time: PENDING -> CONFIRMING. Seats are confirmed in Showtime next; until then the
     * booking stays CONFIRMING (the confirmation job retries).
     */
    public void markConfirming(UUID paymentId, Instant paidAt) {
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(status, "markConfirming");
        }
        if (!wasPaidInTime(paidAt)) {
            throw new InvalidBookingStateException("Thanh toán đến sau khi booking đã hết hạn");
        }
        this.status = BookingStatus.CONFIRMING;
        this.paidPaymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public void markConfirming(UUID paymentId) {
        if (isExpired()) {
            throw new InvalidBookingStateException("Booking đã hết hạn");
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

    /**
     * Paid, but the seats could not be booked (hold expired / seats lost / Showtime down too long):
     * CONFIRMING -> CANCELLED. There is no refund flow; the payment stays SUCCESS and the case is handled manually.
     */
    public void failAfterPayment() {
        if (status != BookingStatus.CONFIRMING) {
            throw new InvalidBookingStateException(status, "failAfterPayment");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
        this.seats.forEach(BookingSeat::cancel);
    }

    /**
     * Marks money that this booking cannot use, for manual refund. Does not change the status and does not touch
     * {@code updatedAt} (the CONFIRMING timeout is measured from it).
     */
    public void flagForRefund(String note) {
        this.needsRefund = true;
        String text = note == null ? "" : note;
        this.refundNote = text.length() > 500 ? text.substring(0, 500) : text;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    private static String generateBookingCode() {
        return "BK" + (System.currentTimeMillis() % 100000000) + (int)(Math.random() * 900 + 100);
    }
}
