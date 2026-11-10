package com.mv.cinemaservice.domain.model.screen.aggregate;

import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;
import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
import com.mv.cinemaservice.domain.model.screen.exception.InvalidScreenException;
import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Screen {
    private UUID id;
    private UUID cinemaId;
    private UUID screenTypeId;
    private String screenTypeName;
    private String name;
    private int totalSeats;
    private ScreenStatus status;

    public boolean isActive() {
        return status == ScreenStatus.ACTIVE;
    }

    public static final int MAX_SEATS = 600;

    /** New screen, ACTIVE. Its seats are built with {@link #buildSeats}. */
    public static Screen create(UUID cinemaId, String name, ScreenType type) {
        if (cinemaId == null) {
            throw new InvalidScreenException("Rạp là bắt buộc");
        }
        Screen screen = Screen.builder()
                .id(UUID.randomUUID())
                .cinemaId(cinemaId)
                .status(ScreenStatus.ACTIVE)
                .build();
        screen.update(name, type);
        return screen;
    }

    /** Rename / change projection type. Allowed in any status. */
    public void update(String newName, ScreenType type) {
        String trimmed = newName == null ? null : newName.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new InvalidScreenException("Tên phòng không được để trống");
        }
        if (trimmed.length() > 100) {
            throw new InvalidScreenException("Tên phòng tối đa 100 ký tự");
        }
        if (type == null) {
            throw new InvalidScreenException("Loại phòng không tồn tại");
        }
        this.name = trimmed;
        this.screenTypeId = type.id();
        this.screenTypeName = type.name();
    }

    /** ACTIVE (sells), MAINTENANCE (temporarily closed, layout editable), INACTIVE (retired). Free transitions. */
    public void changeStatus(ScreenStatus target) {
        if (target == null) {
            throw new InvalidScreenException("Trạng thái là bắt buộc");
        }
        this.status = target;
    }

    /**
     * The layout of an ACTIVE screen cannot change: put it in MAINTENANCE first, so nobody schedules a showtime
     * on a half-edited layout. Existing showtimes keep the seats they copied when they were created.
     */
    public void assertLayoutEditable() {
        if (status == ScreenStatus.ACTIVE) {
            throw new InvalidScreenException("Hãy chuyển phòng sang MAINTENANCE trước khi đổi sơ đồ ghế");
        }
    }

    /**
     * Builds the seats of a layout: row A with 3 seats -> A1, A2, A3. Row labels must be unique, seat types must
     * exist, at most {@value #MAX_SEATS} seats. Also updates {@code totalSeats}.
     */
    public List<Seat> buildSeats(List<SeatRow> rows, Map<UUID, SeatType> seatTypes) {
        if (rows == null || rows.isEmpty()) {
            throw new InvalidScreenException("Phòng chiếu phải có ít nhất một hàng ghế");
        }
        Set<String> labels = new HashSet<>();
        List<Seat> seats = new ArrayList<>();
        for (SeatRow row : rows) {
            if (!labels.add(row.rowLabel())) {
                throw new InvalidScreenException("Hàng " + row.rowLabel() + " bị khai báo trùng");
            }
            SeatType type = seatTypes.get(row.seatTypeId());
            if (type == null) {
                throw new InvalidScreenException("Hàng " + row.rowLabel() + ": loại ghế " + row.seatTypeId() + " không tồn tại");
            }
            for (int n = 1; n <= row.seatCount(); n++) {
                seats.add(Seat.builder()
                        .id(UUID.randomUUID())
                        .screenId(id)
                        .seatTypeId(type.getId())
                        .seatTypeName(type.getName())
                        .priceMultiplier(type.getPriceMultiplier())
                        .rowLabel(row.rowLabel())
                        .seatNumber(n)
                        .label(row.rowLabel() + n)
                        .status(SeatStatus.ACTIVE)
                        .build());
            }
        }
        if (seats.size() > MAX_SEATS) {
            throw new InvalidScreenException("Một phòng tối đa " + MAX_SEATS + " ghế, đang khai báo " + seats.size());
        }
        this.totalSeats = seats.size();
        return seats;
    }
}
