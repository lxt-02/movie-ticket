package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.repository.ShowtimeRepository;
import com.mv.showtimeservice.infrastructure.persistence.entity.ShowtimeEntity;
import com.mv.showtimeservice.infrastructure.persistence.mapper.ShowtimePersistenceMapper;
import com.mv.showtimeservice.infrastructure.persistence.repository.ShowtimeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShowtimeRepositoryAdapter implements ShowtimeRepository, ShowtimeRepositoryPort {

    private final ShowtimeJpaRepository showtimeJpaRepository;
    private final ShowtimePersistenceMapper mapper;

    @Override
    public Optional<Showtime> findById(UUID id) {
        return showtimeJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Showtime> findByIdWithLock(UUID id) {
        return showtimeJpaRepository.findByIdWithLock(id).map(mapper::toDomain);
    }

    @Override
    public Showtime save(Showtime showtime) {
        ShowtimeEntity entity = mapper.toEntity(showtime);
        ShowtimeEntity saved = showtimeJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
