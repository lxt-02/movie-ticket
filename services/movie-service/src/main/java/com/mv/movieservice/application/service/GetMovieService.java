package com.mv.movieservice.application.service;

import com.mv.movieservice.application.port.in.GetMovieUseCase;
import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import lombok.RequiredArgsConstructor;
import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.query.SearchMoviesQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMovieService implements GetMovieUseCase {

    private final LoadMoviePort loadMoviePort;

    @Override
    public Movie getById(GetMovieQuery query) {
        return loadMoviePort.findById(query.getMovieId())
                .orElseThrow(() -> new MovieNotFoundException(query.getMovieId()));
    }

    @Override
    public PageResult<Movie> search(SearchMoviesQuery query) {
        int page = Math.max(0, query.getPage());
        int size = Math.min(100, Math.max(1, query.getSize()));
        String keyword = query.getKeyword() == null || query.getKeyword().isBlank() ? null : query.getKeyword().trim();
        return loadMoviePort.search(query.getStatus(), keyword, query.isIncludeArchived(), page, size);
    }
}
