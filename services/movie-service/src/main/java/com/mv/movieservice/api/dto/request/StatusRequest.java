package com.mv.movieservice.api.dto.request;

import com.mv.movieservice.domain.model.movie.enums.MovieStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusRequest {
    @NotNull(message = "status is required")
    private MovieStatus status;
}
