package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.repository.CinemaRepository;
import com.mv.cinemaservice.infrastructure.persistence.entity.CinemaEntity;
import com.mv.cinemaservice.infrastructure.persistence.mapper.CinemaPersistenceMapper;
import com.mv.cinemaservice.infrastructure.persistence.repository.CinemaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CinemaRepositoryAdapter implements CinemaRepository, LoadCinemaPort {

    private final CinemaJpaRepository cinemaJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public Optional<Cinema> findById(UUID id) {
        return cinemaJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Cinema save(Cinema cinema) {
        CinemaEntity entity = mapper.toEntity(cinema);
        CinemaEntity saved = cinemaJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
