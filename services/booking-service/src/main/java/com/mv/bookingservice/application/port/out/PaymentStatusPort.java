package com.mv.bookingservice.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Direct (synchronous) question to payment-service: is this booking paid?
 * Safety net of the Kafka saga, used by the expiry job before an unpaid booking is expired.
 */
public interface PaymentStatusPort {

    /**
     * @return the SUCCESS payment of the booking, or empty if it has none
     * @throws RuntimeException if payment-service cannot be reached (the caller decides what to do)
     */
    Optional<PaidPayment> findSucceededPayment(UUID bookingId);

    record PaidPayment(UUID paymentId, BigDecimal amount, String currency, Instant paidAt) {
    }
}
