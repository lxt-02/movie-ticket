package com.mv.movieservice.domain.model.movie.valueobject;

import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;

import java.util.UUID;

/**
 * A person working on a movie. {@code name} is the person's name at load time (persons are stored in their own
 * table); {@code characterName} is only meaningful for actors.
 */
public record CrewMember(UUID personId, String name, CrewRole role, String characterName) {

    public CrewMember {
        if (personId == null) {
            throw new InvalidMovieException("Thiếu personId của thành viên đoàn phim");
        }
        if (role == null) {
            throw new InvalidMovieException("Thiếu vai trò (DIRECTOR / ACTOR) của thành viên đoàn phim");
        }
        characterName = characterName == null || characterName.isBlank() ? null : characterName.trim();
        if (role != CrewRole.ACTOR) {
            characterName = null;
        }
        if (characterName != null && characterName.length() > 150) {
            throw new InvalidMovieException("Tên nhân vật tối đa 150 ký tự");
        }
    }
}
