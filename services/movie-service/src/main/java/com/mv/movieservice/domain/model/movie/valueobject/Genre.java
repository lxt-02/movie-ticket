package com.mv.movieservice.domain.model.movie.valueobject;

import java.util.UUID;

/** A movie genre (Action, Drama...). */
public record Genre(UUID id, String name) {
}
