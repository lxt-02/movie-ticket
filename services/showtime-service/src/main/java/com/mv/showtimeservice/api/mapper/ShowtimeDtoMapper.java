package com.mv.showtimeservice.api.mapper;

import com.mv.showtimeservice.api.dto.request.HoldSeatsRequest;
import com.mv.showtimeservice.api.dto.response.HeldSeatResponse;
import com.mv.showtimeservice.api.dto.response.SeatHoldResponse;
import com.mv.showtimeservice.application.command.HoldSeatsCommand;
import com.mv.showtimeservice.application.port.in.HoldSeatsResult;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ShowtimeDtoMapper {

    public HoldSeatsCommand toCommand(UUID showtimeId, HoldSeatsRequest request, String idempotencyKey) {
        if (request == null) {
            return null;
        }

        String requestHash = computeRequestHash(showtimeId, request);

        return HoldSeatsCommand.builder()
                .showtimeId(showtimeId)
                .bookingId(request.getBookingId())
                .showtimeSeatIds(request.getShowtimeSeatIds())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .build();
    }

    public SeatHoldResponse toResponse(HoldSeatsResult result) {
        if (result == null || result.getSeatHold() == null) {
            return null;
        }

        SeatHold hold = result.getSeatHold();
        Showtime showtime = result.getShowtime();
        List<HeldSeatResponse> seatResponses = new ArrayList<>();

        if (result.getSeats() != null) {
            seatResponses = result.getSeats().stream()
                    .map(this::toHeldSeatResponse)
                    .collect(Collectors.toList());
        }

        return SeatHoldResponse.builder()
                .holdId(hold.getId())
                .bookingId(hold.getBookingId())
                .showtimeId(hold.getShowtimeId())
                .status(hold.getStatus())
                .expiresAt(hold.getExpiresAt())
                .movieTitle(showtime != null ? showtime.getMovieTitle() : null)
                .cinemaName(showtime != null ? showtime.getCinemaName() : null)
                .screenName(showtime != null ? showtime.getScreenName() : null)
                .startTime(showtime != null ? showtime.getStartTime() : null)
                .seats(seatResponses)
                .build();
    }

    public SeatHoldResponse toResponse(SeatHold hold, Showtime showtime, List<ShowtimeSeat> seats) {
        if (hold == null) {
            return null;
        }

        List<HeldSeatResponse> seatResponses = new ArrayList<>();
        if (seats != null) {
            seatResponses = seats.stream()
                    .map(this::toHeldSeatResponse)
                    .collect(Collectors.toList());
        }

        return SeatHoldResponse.builder()
                .holdId(hold.getId())
                .bookingId(hold.getBookingId())
                .showtimeId(hold.getShowtimeId())
                .status(hold.getStatus())
                .expiresAt(hold.getExpiresAt())
                .movieTitle(showtime != null ? showtime.getMovieTitle() : null)
                .cinemaName(showtime != null ? showtime.getCinemaName() : null)
                .screenName(showtime != null ? showtime.getScreenName() : null)
                .startTime(showtime != null ? showtime.getStartTime() : null)
                .seats(seatResponses)
                .build();
    }

    public HeldSeatResponse toHeldSeatResponse(ShowtimeSeat seat) {
        if (seat == null) {
            return null;
        }
        return HeldSeatResponse.builder()
                .showtimeSeatId(seat.getId())
                .seatId(seat.getSeatId())
                .seatLabel(seat.getSeatLabel())
                .seatType(seat.getSeatType())
                .price(seat.getPrice())
                .build();
    }

    public String computeRequestHash(UUID showtimeId, HoldSeatsRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(showtimeId).append("|")
              .append(request.getBookingId()).append("|");

            if (request.getShowtimeSeatIds() != null) {
                request.getShowtimeSeatIds().stream()
                        .sorted()
                        .forEach(id -> sb.append(id).append(","));
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
