package com.mv.showtimeservice.api.rest;

import com.mv.showtimeservice.api.dto.request.CreateShowtimeRequest;
import com.mv.showtimeservice.api.dto.response.ApiResponse;
import com.mv.showtimeservice.api.dto.response.ShowtimeResponse;
import com.mv.showtimeservice.application.command.CreateShowtimeCommand;
import com.mv.showtimeservice.application.port.in.CreateShowtimeUseCase;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final CreateShowtimeUseCase createShowtimeUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ShowtimeResponse>> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request
    ) {
        Showtime showtime = createShowtimeUseCase.execute(CreateShowtimeCommand.builder()
                .movieId(request.getMovieId())
                .cinemaId(request.getCinemaId())
                .screenId(request.getScreenId())
                .startTime(request.getStartTime())
                .basePrice(request.getBasePrice())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Showtime created successfully", toResponse(showtime)));
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .screenId(showtime.getScreenId())
                .movieTitle(showtime.getMovieTitle())
                .cinemaId(showtime.getCinemaId())
                .cinemaName(showtime.getCinemaName())
                .screenName(showtime.getScreenName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .status(showtime.getStatus())
                .createdAt(showtime.getCreatedAt())
                .build();
    }
}
