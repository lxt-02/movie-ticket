package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import com.mv.bookingservice.domain.repository.BookingRepository;
import com.mv.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.mv.bookingservice.infrastructure.persistence.mapper.BookingPersistenceMapper;
import com.mv.bookingservice.infrastructure.persistence.repository.BookingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingRepositoryAdapter implements BookingRepository, SaveBookingPort, LoadBookingPort {

    private final BookingJpaRepository bookingJpaRepository;
    private final BookingPersistenceMapper bookingPersistenceMapper;

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity = bookingPersistenceMapper.toEntity(booking);
        BookingEntity saved = bookingJpaRepository.save(entity);
        return bookingPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return bookingJpaRepository.findById(id)
                .map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Booking> findByBookingCode(String bookingCode) {
        return bookingJpaRepository.findByBookingCode(bookingCode)
                .map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey) {
        return bookingJpaRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Booking> findByHoldId(UUID holdId) {
        return bookingJpaRepository.findByHoldId(holdId)
                .map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public List<Booking> findByUserId(UUID userId) {
        return bookingJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(bookingPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant time) {
        return bookingJpaRepository.findByStatusAndExpiresAtBefore(status, time).stream()
                .map(bookingPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID id) {
        return bookingJpaRepository.existsById(id);
    }
}
