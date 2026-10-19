package com.mv.bookingservice.application.port.in;

public interface BookingMaintenanceUseCase {
    /** PENDING bookings past expires_at + grace: EXPIRED, release hold, cancel open payment links. */
    int expireOverdueBookings();

    /** CONFIRMING bookings (paid, seats not confirmed yet): call Showtime confirm again. */
    int retrySeatConfirmations();
}
