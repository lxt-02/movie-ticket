package com.mv.movieservice.domain.model.movie.exception;

import java.util.UUID;

public class MovieNotFoundException extends DomainException {
    public MovieNotFoundException(UUID movieId) {
        super("Không tìm thấy phim có id: " + movieId);
    }
}
