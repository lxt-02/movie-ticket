package com.mv.movieservice.application.port.in;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.query.SearchMoviesQuery;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;

public interface GetMovieUseCase {
    Movie getById(GetMovieQuery query);

    PageResult<Movie> search(SearchMoviesQuery query);
}
