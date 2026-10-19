package com.mv.movieservice.domain.model.movie.aggregate;

import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private UUID id;
    private String title;
    private String originalTitle;
    private String description;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String ageRating;
    private String language;
    private String country;
    private String posterUrl;
    private MovieStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * New movie. Status defaults to COMING_SOON; it may start as NOW_SHOWING but never as ENDED / ARCHIVED.
     */
    public static Movie create(MovieDetails details, MovieStatus initialStatus, Instant now) {
        MovieStatus status = initialStatus == null ? MovieStatus.COMING_SOON : initialStatus;
        if (status != MovieStatus.COMING_SOON && status != MovieStatus.NOW_SHOWING) {
            throw new InvalidMovieException("A new movie must start as COMING_SOON or NOW_SHOWING");
        }
        Movie movie = Movie.builder()
                .id(UUID.randomUUID())
                .status(status)
                .createdAt(now)
                .build();
        movie.apply(details, now);
        return movie;
    }

    /** Replaces the editable details. An archived movie is read-only. */
    public void update(MovieDetails details, Instant now) {
        if (status == MovieStatus.ARCHIVED) {
            throw new InvalidMovieException("An archived movie cannot be edited");
        }
        apply(details, now);
    }

    /**
     * Allowed transitions:
     * <pre>
     * COMING_SOON -> NOW_SHOWING | ARCHIVED
     * NOW_SHOWING -> ENDED | ARCHIVED
     * ENDED       -> NOW_SHOWING (re-release) | ARCHIVED
     * ARCHIVED    -> (none)
     * </pre>
     * Same status: no change.
     */
    public void changeStatus(MovieStatus target, Instant now) {
        if (target == null) {
            throw new InvalidMovieException("status must not be null");
        }
        if (target == status) {
            return;
        }
        boolean allowed = switch (status) {
            case COMING_SOON -> target == MovieStatus.NOW_SHOWING || target == MovieStatus.ARCHIVED;
            case NOW_SHOWING -> target == MovieStatus.ENDED || target == MovieStatus.ARCHIVED;
            case ENDED -> target == MovieStatus.NOW_SHOWING || target == MovieStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) {
            throw new InvalidMovieException("Cannot change movie status from " + status + " to " + target);
        }
        this.status = target;
        this.updatedAt = now;
    }

    /** "Delete" = archive (soft delete): showtimes and bookings keep referring to this movie. Idempotent. */
    public void archive(Instant now) {
        changeStatus(MovieStatus.ARCHIVED, now);
    }

    public boolean isArchived() {
        return status == MovieStatus.ARCHIVED;
    }

    private void apply(MovieDetails details, Instant now) {
        if (details == null) {
            throw new InvalidMovieException("movie details must not be null");
        }
        this.title = details.title();
        this.originalTitle = details.originalTitle();
        this.description = details.description();
        this.durationMinutes = details.durationMinutes();
        this.releaseDate = details.releaseDate();
        this.ageRating = details.ageRating();
        this.language = details.language();
        this.country = details.country();
        this.posterUrl = details.posterUrl();
        this.updatedAt = now;
    }
}
