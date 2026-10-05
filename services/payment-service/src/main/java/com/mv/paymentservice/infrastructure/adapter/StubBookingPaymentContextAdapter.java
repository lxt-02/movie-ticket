package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.application.dto.BookingPaymentContextDto;
import com.mv.paymentservice.application.port.out.BookingPaymentContextPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Stub adapter for BookingPaymentContextPort.
 * This can be replaced by an OpenFeign client adapter when inter-service communication is enabled.
 */
@Component
@ConditionalOnMissingBean(name = "feignBookingPaymentContextAdapter")
public class StubBookingPaymentContextAdapter implements BookingPaymentContextPort {

    @Override
    public Optional<BookingPaymentContextDto> getPaymentContext(UUID bookingId) {
        // Default stub behavior for standalone testing: returns a valid pending booking context
        return Optional.of(BookingPaymentContextDto.builder()
                .bookingId(bookingId)
                .bookingCode("BK-" + bookingId.toString().substring(0, 8).toUpperCase())
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .showtimeId(UUID.randomUUID())
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .status("PENDING")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build());
    }
}
