package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.repository.SeatHoldRepository;
import com.mv.showtimeservice.infrastructure.persistence.entity.SeatHoldEntity;
import com.mv.showtimeservice.infrastructure.persistence.mapper.ShowtimePersistenceMapper;
import com.mv.showtimeservice.infrastructure.persistence.repository.SeatHoldJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatHoldRepositoryAdapter implements SeatHoldRepository, SaveSeatHoldPort, LoadSeatHoldPort {

    private final SeatHoldJpaRepository seatHoldJpaRepository;
    private final ShowtimePersistenceMapper mapper;

    @Override
    public SeatHold save(SeatHold hold) {
        SeatHoldEntity entity = mapper.toEntity(hold);
        SeatHoldEntity saved = seatHoldJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SeatHold> findById(UUID id) {
        return seatHoldJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SeatHold> findByBookingId(UUID bookingId) {
        return seatHoldJpaRepository.findByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public Optional<SeatHold> findByIdempotencyKey(String idempotencyKey) {
        return seatHoldJpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }
}
