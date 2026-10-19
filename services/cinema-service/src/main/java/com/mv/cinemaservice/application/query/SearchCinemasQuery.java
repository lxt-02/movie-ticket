package com.mv.cinemaservice.application.query;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import lombok.Builder;
import lombok.Getter;

/** Cinema list filters. {@code status == null} lists every status except CLOSED unless {@code includeClosed}. */
@Getter
@Builder
public class SearchCinemasQuery {
    private String city;
    private CinemaStatus status;
    private String keyword;
    private boolean includeClosed;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
