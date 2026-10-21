package com.mv.bookingservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Result of getBookingPaymentStatus. Payment fields are null when {@code paid=false}. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentStatusResponse {
    private UUID bookingId;
    private boolean paid;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    /** When the buyer paid (PayOS transaction time). */
    private Instant paidAt;
}
