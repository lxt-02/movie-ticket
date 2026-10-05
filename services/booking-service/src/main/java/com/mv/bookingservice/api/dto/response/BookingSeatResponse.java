package com.mv.bookingservice.api.dto.response;

import com.mv.bookingservice.domain.model.booking.enums.BookingSeatStatus;
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
public class BookingSeatResponse {
    private UUID id;
    private UUID bookingId;
    private UUID showtimeSeatId;
    private UUID seatId;
    private String seatLabel;
    private BigDecimal unitPrice;
    private String ticketCode;
    private BookingSeatStatus status;
}
