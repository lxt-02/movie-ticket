package com.mv.showtimeservice.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CinemaClientPort {
    CinemaSnapshot getCinema(UUID cinemaId);

    ScreenLayoutSnapshot getScreenLayout(UUID screenId);

    record CinemaSnapshot(
            UUID cinemaId,
            String name,
            String status
    ) {
    }

    record ScreenLayoutSnapshot(
            UUID screenId,
            UUID cinemaId,
            String screenName,
            String screenType,
            int totalSeats,
            String status,
            List<SeatSnapshot> seats
    ) {
    }

    record SeatSnapshot(
            UUID seatId,
            String seatType,
            BigDecimal priceMultiplier,
            String label,
            String status
    ) {
    }
}
