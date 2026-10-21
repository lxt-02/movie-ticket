package com.mv.showtimeservice.api.dto.response;

import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One seat of the seat map. Use {@code showtimeSeatId} when creating a booking.
 * {@code rowLabel} / {@code seatNumber} come from the label ("B12" = row "B", seat 12) so the frontend can draw the
 * grid without parsing it; {@code seatNumber} is null if the label has no number.
 */
public record ShowtimeSeatResponse(
        UUID showtimeSeatId,
        UUID seatId,
        String seatLabel,
        String rowLabel,
        Integer seatNumber,
        String seatType,
        BigDecimal price,
        ShowtimeSeatStatus status
) {

    private static final Pattern LABEL = Pattern.compile("^([A-Za-z]+)(\\d+)$");

    public static ShowtimeSeatResponse of(UUID showtimeSeatId, UUID seatId, String seatLabel, String seatType,
                                          BigDecimal price, ShowtimeSeatStatus status) {
        String row = seatLabel;
        Integer number = null;
        Matcher m = seatLabel == null ? null : LABEL.matcher(seatLabel.trim());
        if (m != null && m.matches()) {
            row = m.group(1).toUpperCase();
            number = Integer.valueOf(m.group(2));
        }
        return new ShowtimeSeatResponse(showtimeSeatId, seatId, seatLabel, row, number, seatType, price, status);
    }
}
