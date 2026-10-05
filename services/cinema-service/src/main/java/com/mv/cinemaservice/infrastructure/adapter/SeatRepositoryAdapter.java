package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadSeatPort;
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
public class SeatRepositoryAdapter implements SeatRepository, LoadSeatPort {

    private final SeatJpaRepository seatJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public List<Seat> findByScreenId(UUID screenId) {
        return seatJpaRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
