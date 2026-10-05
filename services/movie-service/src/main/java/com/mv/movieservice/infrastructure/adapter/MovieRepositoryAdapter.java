package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.repository.MovieRepository;
import com.mv.movieservice.infrastructure.persistence.entity.MovieEntity;
import com.mv.movieservice.infrastructure.persistence.mapper.MoviePersistenceMapper;
import com.mv.movieservice.infrastructure.persistence.repository.MovieJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovieRepositoryAdapter implements MovieRepository, LoadMoviePort {

    private final MovieJpaRepository movieJpaRepository;
    private final MoviePersistenceMapper mapper;

    @Override
    public Optional<Movie> findById(UUID id) {
        return movieJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Movie save(Movie movie) {
        MovieEntity entity = mapper.toEntity(movie);
        MovieEntity saved = movieJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
