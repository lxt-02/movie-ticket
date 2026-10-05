package com.mv.bookingservice.api.dto.response;

import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
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
    private String currency;
    private UUID paidPaymentId;
    private Long version;
    private Instant updatedAt;
    private List<BookingSeatResponse> seats;
}
