package com.mv.showtimeservice.application.query;

import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Showtime list filters; every field is optional. Empty {@code statuses} = SCHEDULED + SELLING. */
@Getter
@Builder
public class SearchShowtimesQuery {
    private UUID movieId;
    private UUID cinemaId;
    private UUID screenId;
    /** start time from (inclusive); null = now */
    private Instant from;
    /** start time to (exclusive); null = no upper bound */
    private Instant to;
    private Set<ShowtimeStatus> statuses;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
