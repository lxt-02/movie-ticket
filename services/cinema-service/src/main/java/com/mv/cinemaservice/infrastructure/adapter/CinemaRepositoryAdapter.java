package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.dto.PageResult;
import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.application.port.out.SaveCinemaPort;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class CinemaRepositoryAdapter implements CinemaRepository, LoadCinemaPort, SaveCinemaPort {

    private final CinemaJpaRepository cinemaJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public Optional<Cinema> findById(UUID id) {
        return cinemaJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Cinema> search(String city, CinemaStatus status, String keyword, boolean includeClosed,
                                     int page, int size) {
        Page<CinemaEntity> result = cinemaJpaRepository.search(city, status, keyword, includeClosed,
                CinemaStatus.CLOSED, PageRequest.of(page, size, Sort.by("city").and(Sort.by("name"))));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public Cinema save(Cinema cinema) {
        CinemaEntity entity = mapper.toEntity(cinema);
        CinemaEntity saved = cinemaJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
