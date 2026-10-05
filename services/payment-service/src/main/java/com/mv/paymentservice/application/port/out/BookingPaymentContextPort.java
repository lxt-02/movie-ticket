package com.mv.paymentservice.application.port.out;

import com.mv.paymentservice.application.dto.BookingPaymentContextDto;

import java.util.Optional;
import java.util.UUID;

public interface BookingPaymentContextPort {
    Optional<BookingPaymentContextDto> getPaymentContext(UUID bookingId);
}
