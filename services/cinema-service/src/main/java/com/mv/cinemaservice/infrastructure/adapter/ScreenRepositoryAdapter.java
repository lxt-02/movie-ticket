package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.application.port.out.SaveScreenPort;
import com.mv.cinemaservice.infrastructure.persistence.repository.ScreenTypeJpaRepository;
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
public class ScreenRepositoryAdapter implements ScreenRepository, LoadScreenPort, SaveScreenPort {

    private final ScreenJpaRepository screenJpaRepository;
    private final CinemaJpaRepository cinemaJpaRepository;
    private final ScreenTypeJpaRepository screenTypeJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public Optional<Screen> findById(UUID id) {
        return screenJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public java.util.List<Screen> findByCinemaId(UUID cinemaId) {
        return screenJpaRepository.findByCinemaId(cinemaId).stream()
                .map(mapper::toDomain)
                .sorted(java.util.Comparator.comparing(Screen::getName))
                .toList();
    }

    @Override
    public boolean existsByName(UUID cinemaId, String name, UUID excludeScreenId) {
        return screenJpaRepository.existsByName(cinemaId, name, excludeScreenId);
    }

    @Override
    public Screen save(Screen screen) {
        CinemaEntity cinemaEntity = cinemaJpaRepository.findById(screen.getCinemaId()).orElse(null);
        ScreenTypeEntity screenTypeEntity = screen.getScreenTypeId() != null
                ? screenTypeJpaRepository.findById(screen.getScreenTypeId()).orElse(null)
                : null;
        ScreenEntity entity = mapper.toEntity(screen, cinemaEntity, screenTypeEntity);
        ScreenEntity saved = screenJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
