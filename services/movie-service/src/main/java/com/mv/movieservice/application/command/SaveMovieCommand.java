package com.mv.movieservice.application.command;

import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Create / update a movie. {@code status} is only used on create (initial status). */
@Getter
@Builder
public class SaveMovieCommand {
    private String title;
    private String originalTitle;
    private String description;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String ageRating;
    private String language;
    private String country;
    private String posterUrl;
    private MovieStatus status;
    /** Genres of the movie (replaces the current ones). Null = none. */
    private List<UUID> genreIds;
    /** Directors and actors (replaces the current ones). Null = none. */
    private List<CrewInput> crew;

    public record CrewInput(UUID personId, CrewRole role, String characterName) {
    }
}
