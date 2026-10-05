package com.mv.movieservice.application.service;

import com.mv.movieservice.application.port.out.LoadMoviePort;
import com.mv.movieservice.application.query.GetMovieQuery;
import com.mv.movieservice.domain.model.movie.aggregate.Movie;
import com.mv.movieservice.domain.model.movie.exception.MovieNotFoundException;
import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMovieServiceTest {

    @Mock
    private LoadMoviePort loadMoviePort;

    private GetMovieService getMovieService;

    @BeforeEach
    void setUp() {
        getMovieService = new GetMovieService(loadMoviePort);
    }

    @Test
    @DisplayName("Should return movie successfully when found")
    void shouldReturnMovieWhenFound() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder()
                .id(movieId)
                .title("Inception")
                .originalTitle("Inception")
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .status(MovieStatus.NOW_SHOWING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(loadMoviePort.findById(movieId)).thenReturn(Optional.of(movie));

        Movie result = getMovieService.getById(new GetMovieQuery(movieId));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(movieId);
        assertThat(result.getTitle()).isEqualTo("Inception");
        assertThat(result.getDurationMinutes()).isEqualTo(148);
    }

    @Test
    @DisplayName("Should throw MovieNotFoundException when movie does not exist")
    void shouldThrowExceptionWhenNotFound() {
        UUID movieId = UUID.randomUUID();
        when(loadMoviePort.findById(movieId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMovieService.getById(new GetMovieQuery(movieId)))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining(movieId.toString());
    }
}
