package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.port.out.SaveMoviePort;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class MovieRepositoryAdapter implements MovieRepository, LoadMoviePort, SaveMoviePort {

    private final MovieJpaRepository movieJpaRepository;
    private final MoviePersistenceMapper mapper;

    @Override
    public Optional<Movie> findById(UUID id) {
        return movieJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Movie> search(MovieStatus status, String keyword, boolean includeArchived, int page, int size) {
        Page<MovieEntity> result = movieJpaRepository.search(status, keyword, includeArchived, MovieStatus.ARCHIVED,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseDate").and(Sort.by("title"))));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public Movie save(Movie movie) {
        MovieEntity entity = mapper.toEntity(movie);
        MovieEntity saved = movieJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
