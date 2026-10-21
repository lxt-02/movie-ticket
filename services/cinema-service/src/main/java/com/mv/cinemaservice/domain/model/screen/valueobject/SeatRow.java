package com.mv.cinemaservice.domain.model.screen.valueobject;

import com.mv.cinemaservice.domain.model.screen.exception.InvalidScreenException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One row of the seat layout: "row A has 12 STANDARD seats" -> seats A1..A12.
 *
 * @param rowLabel  1-2 uppercase letters (A, B, ..., AA)
 * @param seatCount 1..50 seats
 */
public record SeatRow(String rowLabel, int seatCount, UUID seatTypeId) {

    public static final int MAX_SEATS_PER_ROW = 50;
    private static final Pattern ROW = Pattern.compile("^[A-Z]{1,2}$");

    public SeatRow {
        rowLabel = rowLabel == null ? null : rowLabel.trim().toUpperCase();
        if (rowLabel == null || !ROW.matcher(rowLabel).matches()) {
            throw new InvalidScreenException("Tên hàng ghế phải là 1–2 chữ cái, nhận được: " + rowLabel);
        }
        if (seatCount < 1 || seatCount > MAX_SEATS_PER_ROW) {
            throw new InvalidScreenException("Hàng " + rowLabel + ": số ghế phải từ 1 đến " + MAX_SEATS_PER_ROW);
        }
        if (seatTypeId == null) {
            throw new InvalidScreenException("Hàng " + rowLabel + ": loại ghế là bắt buộc");
        }
    }
}
