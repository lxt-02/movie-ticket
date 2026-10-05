package com.mv.bookingservice.api.mapper;

import com.mv.bookingservice.api.dto.request.CreateBookingRequest;
import com.mv.bookingservice.api.dto.request.CreateBookingSeatRequest;
import com.mv.bookingservice.api.dto.response.BookingPaymentContextResponse;
import com.mv.bookingservice.api.dto.response.BookingResponse;
import com.mv.bookingservice.api.dto.response.BookingSeatResponse;
import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.command.CreateBookingSeatCommand;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BookingDtoMapper {

    public CreateBookingCommand toCommand(CreateBookingRequest request, UUID resolvedUserId, String idempotencyKey) {
        if (request == null) {
            return null;
        }
        UUID userId = resolvedUserId != null ? resolvedUserId : request.getUserId();
        String requestHash = computeRequestHash(request);

        List<CreateBookingSeatCommand> seats = new ArrayList<>();
        if (request.getSeats() != null) {
            seats = request.getSeats().stream()
                    .map(this::toSeatCommand)
                    .collect(Collectors.toList());
        }

        return CreateBookingCommand.builder()
                .userId(userId)
                .showtimeId(request.getShowtimeId())
                .movieTitle(request.getMovieTitle())
                .cinemaName(request.getCinemaName())
                .screenName(request.getScreenName())
                .startTime(request.getStartTime())
                .holdId(request.getHoldId())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .expiresAt(request.getExpiresAt())
                .discountAmount(request.getDiscountAmount())
                .seats(seats)
                .build();
    }

    public CreateBookingSeatCommand toSeatCommand(CreateBookingSeatRequest request) {
        if (request == null) {
            return null;
        }
        return CreateBookingSeatCommand.builder()
                .showtimeSeatId(request.getShowtimeSeatId())
                .seatId(request.getSeatId())
                .seatLabel(request.getSeatLabel())
                .unitPrice(request.getUnitPrice())
                .build();
    }

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingSeatResponse> seats = new ArrayList<>();
        if (booking.getSeats() != null) {
            seats = booking.getSeats().stream()
                    .map(this::toSeatResponse)
                    .collect(Collectors.toList());
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .movieTitle(booking.getMovieTitle())
                .cinemaName(booking.getCinemaName())
                .screenName(booking.getScreenName())
                .startTime(booking.getStartTime())
                .subtotal(booking.getSubtotal())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .holdId(booking.getHoldId())
                .currency(booking.getCurrency())
                .paidPaymentId(booking.getPaidPaymentId())
                .version(booking.getVersion())
                .updatedAt(booking.getUpdatedAt())
                .seats(seats)
                .build();
    }

    public BookingSeatResponse toSeatResponse(BookingSeat seat) {
        if (seat == null) {
            return null;
        }
        return BookingSeatResponse.builder()
                .id(seat.getId())
                .bookingId(seat.getBookingId())
                .showtimeSeatId(seat.getShowtimeSeatId())
                .seatId(seat.getSeatId())
                .seatLabel(seat.getSeatLabel())
                .unitPrice(seat.getUnitPrice())
                .ticketCode(seat.getTicketCode())
                .status(seat.getStatus())
                .build();
    }

    public BookingPaymentContextResponse toPaymentContextResponse(Booking booking) {
        if (booking == null) {
            return null;
        }
        return BookingPaymentContextResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .amount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .build();
    }

    public String computeRequestHash(CreateBookingRequest request) {
        try {
            // Build normalized payload representation
            StringBuilder sb = new StringBuilder();
            sb.append(request.getShowtimeId()).append("|")
              .append(request.getHoldId()).append("|")
              .append(request.getDiscountAmount()).append("|");

            if (request.getSeats() != null) {
                request.getSeats().stream()
                        .sorted((a, b) -> a.getShowtimeSeatId().compareTo(b.getShowtimeSeatId()))
                        .forEach(s -> sb.append(s.getShowtimeSeatId()).append(":").append(s.getUnitPrice()).append(","));
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
