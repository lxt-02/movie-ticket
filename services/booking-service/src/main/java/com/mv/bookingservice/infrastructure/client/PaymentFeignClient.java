package com.mv.bookingservice.infrastructure.client;

import com.mv.bookingservice.infrastructure.client.dto.BookingPaymentStatusResponse;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client of payment-service (endpoint of payment's InternalPaymentController).
 * Safety net of the Kafka saga: before an unpaid booking is expired, payment-service is asked directly,
 * so a PAYMENT_SUCCEEDED event that was lost or is still waiting in the dead-letter topic cannot cost a paid seat.
 */
@FeignClient(name = "payment-service", url = "${clients.payment.base-url:}")
public interface PaymentFeignClient {

    /** Whether the booking has a SUCCESS payment. Always 200; {@code paid=false} when there is none. */
    @GetMapping("/internal/payments/by-booking/{bookingId}/paid-status")
    RemoteResponse<BookingPaymentStatusResponse> getBookingPaymentStatus(@PathVariable("bookingId") UUID bookingId);
}
