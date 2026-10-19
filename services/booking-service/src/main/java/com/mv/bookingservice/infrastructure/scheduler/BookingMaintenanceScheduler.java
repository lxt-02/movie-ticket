package com.mv.bookingservice.infrastructure.scheduler;

import com.mv.bookingservice.application.port.in.BookingMaintenanceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingMaintenanceScheduler {

    private final BookingMaintenanceUseCase bookingMaintenanceUseCase;

    @Scheduled(
            fixedDelayString = "${booking.expiry.delay-ms:30000}",
            initialDelayString = "${booking.expiry.initial-delay-ms:30000}"
    )
    public void expireOverdueBookings() {
        try {
            int expired = bookingMaintenanceUseCase.expireOverdueBookings();
            if (expired > 0) {
                log.info("Expired {} overdue booking(s)", expired);
            }
        } catch (RuntimeException e) {
            log.error("Booking expiry run failed", e);
        }
    }

    @Scheduled(
            fixedDelayString = "${booking.confirmation-retry.delay-ms:60000}",
            initialDelayString = "${booking.confirmation-retry.initial-delay-ms:45000}"
    )
    public void retrySeatConfirmations() {
        try {
            int confirmed = bookingMaintenanceUseCase.retrySeatConfirmations();
            if (confirmed > 0) {
                log.info("Confirmed seats of {} CONFIRMING booking(s)", confirmed);
            }
        } catch (RuntimeException e) {
            log.error("Seat confirmation retry run failed", e);
        }
    }
}
