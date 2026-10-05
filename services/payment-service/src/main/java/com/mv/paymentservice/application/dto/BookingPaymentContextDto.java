package com.mv.paymentservice.application.dto;

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
public class BookingPaymentContextDto {
    private UUID bookingId;
    private String bookingCode;
    private UUID userId;
    private UUID showtimeId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant expiresAt;
}
