package com.mv.showtimeservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SeatHoldItemId implements Serializable {

    @Column(name = "hold_id", nullable = false)
    private UUID holdId;

    @Column(name = "showtime_seat_id", nullable = false)
    private UUID showtimeSeatId;
}
