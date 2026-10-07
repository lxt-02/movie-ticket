package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.repository.ShowtimeSeatRepository;
import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeSeatEntity;
import com.mv.showtimeservice.infrastructure.persistence.mapper.ShowtimePersistenceMapper;
import com.mv.showtimeservice.infrastructure.persistence.repository.ShowtimeSeatJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ShowtimeSeatRepositoryAdapter implements ShowtimeSeatRepository, ShowtimeSeatRepositoryPort {

    private final ShowtimeSeatJpaRepository showtimeSeatJpaRepository;
    private final ShowtimePersistenceMapper mapper;

    @Override
    public List<ShowtimeSeat> findByShowtimeId(UUID showtimeId) {
        return showtimeSeatJpaRepository.findByShowtimeId(showtimeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowtimeSeat> findByIdsWithLock(List<UUID> ids) {
        return showtimeSeatJpaRepository.findByIdsWithLock(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowtimeSeat> findByHoldId(UUID holdId) {
        return showtimeSeatJpaRepository.findByHoldId(holdId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowtimeSeat> saveAll(List<ShowtimeSeat> seats) {
        List<ShowtimeSeatEntity> entities = seats.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        List<ShowtimeSeatEntity> saved = showtimeSeatJpaRepository.saveAll(entities);
        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ShowtimeSeat> findById(UUID id) {
        return showtimeSeatJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByShowtimeId(UUID showtimeId) {
        return showtimeSeatJpaRepository.existsByShowtimeId(showtimeId);
    }
}
