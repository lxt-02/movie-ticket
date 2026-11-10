package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadSeatPort;
import com.mv.cinemaservice.application.port.out.SaveSeatPort;
import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.SeatEntity;
import com.mv.cinemaservice.infrastructure.persistence.repository.ScreenJpaRepository;
import com.mv.cinemaservice.infrastructure.persistence.repository.SeatTypeJpaRepository;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.repository.SeatRepository;
import com.mv.cinemaservice.infrastructure.persistence.mapper.CinemaPersistenceMapper;
import com.mv.cinemaservice.infrastructure.persistence.repository.SeatJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SeatRepositoryAdapter implements SeatRepository, LoadSeatPort, SaveSeatPort {

    private final SeatJpaRepository seatJpaRepository;
    private final CinemaPersistenceMapper mapper;
    private final ScreenJpaRepository screenJpaRepository;
    private final SeatTypeJpaRepository seatTypeJpaRepository;

    @Override
    public List<Seat> findByScreenId(UUID screenId) {
        return seatJpaRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.Optional<Seat> findById(UUID seatId) {
        return seatJpaRepository.findById(seatId).map(mapper::toDomain);
    }

    @Override
    public List<Seat> replaceSeats(UUID screenId, List<Seat> seats) {
        seatJpaRepository.deleteByScreenId(screenId);
        ScreenEntity screen = screenJpaRepository.getReferenceById(screenId);
        List<SeatEntity> entities = seats.stream()
                .map(seat -> mapper.toEntity(seat, screen, seatTypeJpaRepository.getReferenceById(seat.getSeatTypeId())))
                .toList();
        seatJpaRepository.saveAll(entities);
        return findByScreenId(screenId);
    }

    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = mapper.toEntity(seat, screenJpaRepository.getReferenceById(seat.getScreenId()),
                seatTypeJpaRepository.getReferenceById(seat.getSeatTypeId()));
        return mapper.toDomain(seatJpaRepository.save(entity));
    }
}
