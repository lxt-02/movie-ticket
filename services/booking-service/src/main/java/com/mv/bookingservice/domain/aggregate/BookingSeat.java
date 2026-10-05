package com.mv.bookingservice.domain.aggregate;

import com.mv.bookingservice.domain.model.BookingSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeat {
    private UUID id;
    private UUID bookingId;
    private UUID showtimeSeatId;
    private UUID seatId;
    private String seatLabel;
    private BigDecimal unitPrice;
    private String ticketCode;
    private BookingSeatStatus status;

    public static BookingSeat create(UUID bookingId, UUID showtimeSeatId, UUID seatId, String seatLabel, BigDecimal unitPrice) {
        if (showtimeSeatId == null) {
            throw new IllegalArgumentException("showtimeSeatId must not be null");
        }
        if (seatId == null) {
            throw new IllegalArgumentException("seatId must not be null");
        }
        if (seatLabel == null || seatLabel.isBlank()) {
            throw new IllegalArgumentException("seatLabel must not be blank");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must be greater than or equal to 0");
        }

        String ticketCode = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        return BookingSeat.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .showtimeSeatId(showtimeSeatId)
                .seatId(seatId)
                .seatLabel(seatLabel)
                .unitPrice(unitPrice)
                .ticketCode(ticketCode)
                .status(BookingSeatStatus.ACTIVE)
                .build();
    }
}
