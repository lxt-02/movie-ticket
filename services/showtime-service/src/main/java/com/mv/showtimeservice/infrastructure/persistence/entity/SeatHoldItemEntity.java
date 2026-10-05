package com.mv.showtimeservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seat_hold_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldItemEntity {

    @EmbeddedId
    private SeatHoldItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("holdId")
    @JoinColumn(name = "hold_id", nullable = false)
    private SeatHoldEntity seatHold;

    @Column(name = "showtime_id", nullable = false)
    private UUID showtimeId;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
