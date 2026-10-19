package com.mv.showtimeservice.api.mapper;

import com.mv.showtimeservice.api.dto.request.HoldSeatsRequest;
import com.mv.showtimeservice.application.command.HoldSeatsCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShowtimeDtoMapperTest {

    private final ShowtimeDtoMapper mapper = new ShowtimeDtoMapper();

    @Test
    void shouldDeriveHoldIdempotencyKeyFromBookingId() {
        UUID showtimeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .bookingId(bookingId)
                .showtimeSeatIds(List.of(UUID.randomUUID()))
                .build();

        HoldSeatsCommand command = mapper.toCommand(showtimeId, request);

        assertEquals("booking:" + bookingId, command.getIdempotencyKey());
    }

    @Test
    void differentBookingsNeverShareHoldKey() {
        UUID showtimeId = UUID.randomUUID();
        List<UUID> seats = List.of(UUID.randomUUID());
        HoldSeatsCommand a = mapper.toCommand(showtimeId, HoldSeatsRequest.builder()
                .bookingId(UUID.randomUUID()).showtimeSeatIds(seats).build());
        HoldSeatsCommand b = mapper.toCommand(showtimeId, HoldSeatsRequest.builder()
                .bookingId(UUID.randomUUID()).showtimeSeatIds(seats).build());

        assertNotEquals(a.getIdempotencyKey(), b.getIdempotencyKey());
    }

    @Test
    void sameBookingAndSeatsProduceSameRequestHashRegardlessOfSeatOrder() {
        UUID showtimeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();

        HoldSeatsCommand first = mapper.toCommand(showtimeId, HoldSeatsRequest.builder()
                .bookingId(bookingId).showtimeSeatIds(List.of(s1, s2)).build());
        HoldSeatsCommand retry = mapper.toCommand(showtimeId, HoldSeatsRequest.builder()
                .bookingId(bookingId).showtimeSeatIds(List.of(s2, s1)).build());

        assertEquals(first.getRequestHash(), retry.getRequestHash());
        assertEquals(first.getIdempotencyKey(), retry.getIdempotencyKey());
    }
}
