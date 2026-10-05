package com.mv.bookingservice.domain.repository;

import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<Booking> findByHoldId(UUID holdId);

    List<Booking> findByUserId(UUID userId);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant time);

    boolean existsById(UUID id);
}
