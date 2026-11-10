package com.mv.movieservice.domain.model.movie.aggregate;

import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.valueobject.CrewMember;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.MovieDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    public static final int MAX_GENRES = 10;
    public static final int MAX_CREW = 50;

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
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();
    @Builder.Default
    private List<CrewMember> crew = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * New movie. Status defaults to COMING_SOON; it may start as NOW_SHOWING but never as ENDED / ARCHIVED.
     */
    public static Movie create(MovieDetails details, MovieStatus initialStatus, Instant now) {
        return create(details, List.of(), List.of(), initialStatus, now);
    }

    public static Movie create(MovieDetails details, List<Genre> genres, List<CrewMember> crew,
                               MovieStatus initialStatus, Instant now) {
        MovieStatus status = initialStatus == null ? MovieStatus.COMING_SOON : initialStatus;
        if (status != MovieStatus.COMING_SOON && status != MovieStatus.NOW_SHOWING) {
            throw new InvalidMovieException("Phim mới chỉ được ở trạng thái COMING_SOON hoặc NOW_SHOWING");
        }
        Movie movie = Movie.builder()
                .id(UUID.randomUUID())
                .status(status)
                .createdAt(now)
                .build();
        movie.apply(details, now);
        movie.assignCatalogue(genres, crew);
        return movie;
    }

    /** Replaces the editable details, keeping genres and crew. An archived movie is read-only. */
    public void update(MovieDetails details, Instant now) {
        update(details, genres, crew, now);
    }

    /** Replaces the editable details, genres and crew (PUT semantics). An archived movie is read-only. */
    public void update(MovieDetails details, List<Genre> genres, List<CrewMember> crew, Instant now) {
        if (status == MovieStatus.ARCHIVED) {
            throw new InvalidMovieException("Phim đã lưu trữ (ARCHIVED) không được sửa");
        }
        apply(details, now);
        assignCatalogue(genres, crew);
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
     *
     * @return true if the status changed
     */
    public boolean changeStatus(MovieStatus target, Instant now) {
        if (target == null) {
            throw new InvalidMovieException("Trạng thái là bắt buộc");
        }
        if (target == status) {
            return false;
        }
        boolean allowed = switch (status) {
            case COMING_SOON -> target == MovieStatus.NOW_SHOWING || target == MovieStatus.ARCHIVED;
            case NOW_SHOWING -> target == MovieStatus.ENDED || target == MovieStatus.ARCHIVED;
            case ENDED -> target == MovieStatus.NOW_SHOWING || target == MovieStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) {
            throw new InvalidMovieException("Không thể chuyển trạng thái phim từ " + status + " sang " + target);
        }
        this.status = target;
        this.updatedAt = now;
        return true;
    }

    /**
     * "Delete" = archive (soft delete): showtimes and bookings keep referring to this movie. Idempotent.
     *
     * @return true if the movie was not archived yet
     */
    public boolean archive(Instant now) {
        return changeStatus(MovieStatus.ARCHIVED, now);
    }

    public boolean isArchived() {
        return status == MovieStatus.ARCHIVED;
    }

    public List<CrewMember> directors() {
        return crew.stream().filter(c -> c.role() == CrewRole.DIRECTOR).toList();
    }

    public List<CrewMember> actors() {
        return crew.stream().filter(c -> c.role() == CrewRole.ACTOR).toList();
    }

    private void apply(MovieDetails details, Instant now) {
        if (details == null) {
            throw new InvalidMovieException("Thiếu thông tin phim");
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

    private void assignCatalogue(List<Genre> newGenres, List<CrewMember> newCrew) {
        List<Genre> genreList = newGenres == null ? List.of() : newGenres;
        List<CrewMember> crewList = newCrew == null ? List.of() : newCrew;
        if (genreList.size() > MAX_GENRES) {
            throw new InvalidMovieException("Một phim tối đa " + MAX_GENRES + " thể loại");
        }
        if (crewList.size() > MAX_CREW) {
            throw new InvalidMovieException("Một phim tối đa " + MAX_CREW + " thành viên đoàn phim");
        }
        Set<UUID> genreIds = new HashSet<>();
        for (Genre genre : genreList) {
            if (!genreIds.add(genre.id())) {
                throw new InvalidMovieException("Thể loại bị trùng: " + genre.name());
            }
        }
        Set<String> crewKeys = new HashSet<>();
        for (CrewMember member : crewList) {
            if (!crewKeys.add(member.personId() + ":" + member.role())) {
                throw new InvalidMovieException("Thành viên đoàn phim bị trùng: " + member.personId() + " (" + member.role() + ")");
            }
        }
        this.genres = new ArrayList<>(genreList);
        this.crew = new ArrayList<>(crewList);
    }
}
