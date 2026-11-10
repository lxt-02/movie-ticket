package com.mv.movieservice.domain.model.movie.valueobject;

import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;

import java.util.UUID;

/** A director or actor that can be attached to movies. */
public record Person(UUID id, String name, String avatarUrl, String biography) {

    public Person {
        name = name == null ? null : name.trim();
        if (name == null || name.isEmpty()) {
            throw new InvalidMovieException("Tên người không được để trống");
        }
        if (name.length() > 150) {
            throw new InvalidMovieException("Tên người tối đa 150 ký tự");
        }
        avatarUrl = avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl.trim();
        biography = biography == null || biography.isBlank() ? null : biography.trim();
    }
}
