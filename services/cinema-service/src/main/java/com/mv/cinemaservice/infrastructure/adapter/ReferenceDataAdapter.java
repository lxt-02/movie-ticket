package com.mv.cinemaservice.infrastructure.adapter;

import com.mv.cinemaservice.application.port.out.LoadReferenceDataPort;
import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;
import com.mv.cinemaservice.infrastructure.persistence.mapper.CinemaPersistenceMapper;
import com.mv.cinemaservice.infrastructure.persistence.repository.ScreenTypeJpaRepository;
import com.mv.cinemaservice.infrastructure.persistence.repository.SeatTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReferenceDataAdapter implements LoadReferenceDataPort {

    private final ScreenTypeJpaRepository screenTypeJpaRepository;
    private final SeatTypeJpaRepository seatTypeJpaRepository;
    private final CinemaPersistenceMapper mapper;

    @Override
    public List<ScreenType> findAllScreenTypes() {
        return screenTypeJpaRepository.findAll(Sort.by("name")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ScreenType> findScreenType(UUID id) {
        return screenTypeJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SeatType> findAllSeatTypes() {
        return seatTypeJpaRepository.findAll(Sort.by("priceMultiplier")).stream().map(mapper::toDomain).toList();
    }
}
