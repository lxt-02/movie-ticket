package com.mv.movieservice.application.query;

import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Movie list filters. {@code status == null} lists every status except ARCHIVED unless {@code includeArchived}.
 */
@Getter
@Builder
public class SearchMoviesQuery {
    private MovieStatus status;
    private String keyword;
    private boolean includeArchived;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
