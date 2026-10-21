package com.mv.bookingservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/** Body of holdSeats. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsRequest {
    private UUID bookingId;
    private List<UUID> showtimeSeatIds;
}
