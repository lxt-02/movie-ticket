package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.PaymentStatusPort;
import com.mv.bookingservice.infrastructure.client.PaymentFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.BookingPaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Asks payment-service whether a booking is paid. No retry / circuit breaker here: the caller is a background
 * job that simply tries again on its next run when payment-service is unreachable.
 */
@Component
@RequiredArgsConstructor
public class PaymentStatusAdapter implements PaymentStatusPort {

    private final PaymentFeignClient paymentFeignClient;

    @Override
    public Optional<PaidPayment> findSucceededPayment(UUID bookingId) {
        RemoteResponse<BookingPaymentStatusResponse> response = paymentFeignClient.getBookingPaymentStatus(bookingId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Payment service returned no payment status for booking " + bookingId);
        }
        BookingPaymentStatusResponse status = response.getData();
        if (!status.isPaid()) {
            return Optional.empty();
        }
        return Optional.of(new PaidPayment(status.getPaymentId(), status.getAmount(), status.getCurrency(),
                status.getPaidAt()));
    }
}
