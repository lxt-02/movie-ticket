package com.mv.movieservice.api.dto.request;

import com.mv.movieservice.domain.model.movie.enums.CrewRole;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Body of POST /api/v1/movies and PUT /api/v1/movies/{id} (PUT replaces every field, genres and crew included). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "Tên phim là bắt buộc")
    @Size(max = 255, message = "Tên phim tối đa 255 ký tự")
    private String title;

    @Size(max = 255, message = "Tên gốc tối đa 255 ký tự")
    private String originalTitle;

    private String description;

    @NotNull(message = "Thời lượng phim là bắt buộc")
    @Min(value = 1, message = "Thời lượng phim tối thiểu 1 phút")
    @Max(value = 600, message = "Thời lượng phim tối đa 600 phút")
    private Integer durationMinutes;

    private LocalDate releaseDate;

    @Pattern(regexp = "(?i)P|K|T13|T16|T18", message = "Phân loại độ tuổi phải là P, K, T13, T16 hoặc T18")
    private String ageRating;

    @Size(max = 50, message = "Ngôn ngữ tối đa 50 ký tự")
    private String language;

    @Size(max = 100, message = "Quốc gia tối đa 100 ký tự")
    private String country;

    private String posterUrl;

    /** Create only: COMING_SOON (default) or NOW_SHOWING. Ignored on update; use PATCH /status. */
    private MovieStatus status;

    /** Genre ids (GET /api/v1/movies/genres). Empty or missing = no genre. */
    @Size(max = 10, message = "Một phim tối đa 10 thể loại")
    private List<UUID> genreIds;

    /** Directors and actors (persons from GET /api/v1/movies/persons). */
    @Valid
    @Size(max = 50, message = "Một phim tối đa 50 thành viên đoàn phim")
    private List<CrewRequest> crew;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrewRequest {

        @NotNull(message = "personId là bắt buộc")
        private UUID personId;

        @NotNull(message = "role là bắt buộc (DIRECTOR hoặc ACTOR)")
        private CrewRole role;

        /** Actors only: the character played. */
        @Size(max = 150, message = "Tên nhân vật tối đa 150 ký tự")
        private String characterName;
    }
}
