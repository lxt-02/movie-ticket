package com.mv.bookingservice.infrastructure.config;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class BookingPaymentConfig {

    @Bean
    public BookingPaymentPolicy bookingPaymentPolicy(
            @Value("${booking.payment.max-attempts:3}") int maxAttempts,
            @Value("${booking.expiry.grace-seconds:90}") long graceSeconds,
            @Value("${booking.expiry.batch-size:50}") int batchSize,
            @Value("${booking.confirmation-retry.timeout-minutes:30}") long confirmationTimeoutMinutes,
            @Value("${booking.payment.hold-safety-margin-seconds:180}") long holdSafetyMarginSeconds
    ) {
        return new BookingPaymentPolicy(
                maxAttempts > 0 ? maxAttempts : 3,
                Duration.ofSeconds(Math.max(0, graceSeconds)),
                batchSize > 0 ? batchSize : 50,
                Duration.ofMinutes(confirmationTimeoutMinutes > 0 ? confirmationTimeoutMinutes : 30),
                Duration.ofSeconds(holdSafetyMarginSeconds)
        );
    }
}
