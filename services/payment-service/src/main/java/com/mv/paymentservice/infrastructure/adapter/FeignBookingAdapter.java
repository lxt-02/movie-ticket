package com.mv.paymentservice.infrastructure.adapter;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.application.dto.BookingPaymentContextDto;
import com.mv.paymentservice.application.port.out.BookingConfirmationPort;
import com.mv.paymentservice.application.port.out.BookingPaymentContextPort;
import com.mv.paymentservice.domain.model.payment.exception.InvalidPaymentException;
import com.mv.paymentservice.infrastructure.client.BookingFeignClient;
import com.mv.paymentservice.infrastructure.client.dto.ConfirmBookingPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component("feignBookingPaymentContextAdapter")
@RequiredArgsConstructor
public class FeignBookingAdapter implements BookingPaymentContextPort, BookingConfirmationPort {

    private final BookingFeignClient bookingFeignClient;

    @Override
    public Optional<BookingPaymentContextDto> getPaymentContext(UUID bookingId, UUID userId) {
        ApiResponse<BookingPaymentContextDto> response = bookingFeignClient.getPaymentContext(bookingId, userId);
        if (response == null || !response.isSuccess()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.getData());
    }

    @Override
    public void confirmPaymentSucceeded(UUID bookingId, UUID paymentId, BigDecimal amount, String currency) {
        ConfirmBookingPaymentRequest request = ConfirmBookingPaymentRequest.builder()
                .paymentId(paymentId)
                .amount(amount)
                .currency(currency)
                .build();
        ApiResponse<Object> response = bookingFeignClient.confirmPaymentSucceeded(bookingId, request);
        if (response == null || !response.isSuccess()) {
            String message = response != null ? response.getMessage() : "empty response";
            throw new InvalidPaymentException("Booking service failed to confirm payment: " + message);
        }
    }
}
