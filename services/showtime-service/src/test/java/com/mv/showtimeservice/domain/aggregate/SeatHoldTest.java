package com.mv.showtimeservice.domain.aggregate;

import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SeatHoldTest {

    @Test
    void shouldCreateSeatHoldSuccessfully() {
        UUID bookingId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);

        SeatHoldItem item = SeatHoldItem.builder()
                .showtimeSeatId(UUID.randomUUID())
                .unitPrice(new BigDecimal("100000.00"))
                .build();

        SeatHold hold = SeatHold.create(
                bookingId,
                showtimeId,
                "idemp-key-1",
                "request-hash-1",
                expiresAt,
                List.of(item)
        );

        assertNotNull(hold.getId());
        assertEquals(bookingId, hold.getBookingId());
        assertEquals(showtimeId, hold.getShowtimeId());
        assertEquals(SeatHoldStatus.HELD, hold.getStatus());
        assertEquals(1, hold.getItems().size());
        assertEquals(hold.getId(), hold.getItems().get(0).getHoldId());
        assertEquals(showtimeId, hold.getItems().get(0).getShowtimeId());
    }

    @Test
    void shouldConfirmSeatHold() {
        SeatHold hold = SeatHold.builder()
                .id(UUID.randomUUID())
                .status(SeatHoldStatus.HELD)
                .build();

        hold.confirm();

        assertEquals(SeatHoldStatus.CONFIRMED, hold.getStatus());
        assertNotNull(hold.getUpdatedAt());
    }

    @Test
    void shouldReleaseSeatHold() {
        SeatHold hold = SeatHold.builder()
                .id(UUID.randomUUID())
                .status(SeatHoldStatus.HELD)
                .build();

        hold.release();

        assertEquals(SeatHoldStatus.RELEASED, hold.getStatus());
        assertNotNull(hold.getUpdatedAt());
    }
}
