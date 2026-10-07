package com.mv.showtimeservice.application.port.out;

import java.util.UUID;

public interface MovieClientPort {
    MovieSnapshot getMovie(UUID movieId);

    record MovieSnapshot(
            UUID movieId,
            String title,
            int durationMinutes,
            String ageRating,
            String status
    ) {
    }
}
