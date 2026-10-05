package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.repository.ScreenRepository;
import com.mv.cinemaservice.infrastructure.persistence.entity.CinemaEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenEntity;
import com.mv.cinemaservice.infrastructure.persistence.entity.ScreenTypeEntity;
import com.mv.cinemaservice.infrastructure.persistence.mapper.CinemaPersistenceMapper;
import com.mv.cinemaservice.infrastructure.persistence.repository.CinemaJpaRepository;
import com.mv.cinemaservice.infrastructure.persistence.repository.ScreenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScreenRepositoryAdapter implements ScreenRepository, LoadScreenPort {

    private final ScreenJpaRepository screenJpaRepository;
    private final CinemaJpaRepository cinemaJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public Optional<Screen> findById(UUID id) {
        return screenJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Screen save(Screen screen) {
        CinemaEntity cinemaEntity = cinemaJpaRepository.findById(screen.getCinemaId()).orElse(null);
        ScreenTypeEntity screenTypeEntity = screen.getScreenTypeId() != null
                ? ScreenTypeEntity.builder().id(screen.getScreenTypeId()).name(screen.getScreenTypeName()).build()
                : null;
        ScreenEntity entity = mapper.toEntity(screen, cinemaEntity, screenTypeEntity);
        ScreenEntity saved = screenJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
