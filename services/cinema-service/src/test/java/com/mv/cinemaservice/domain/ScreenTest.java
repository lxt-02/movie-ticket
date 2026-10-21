package com.mv.cinemaservice.domain;

import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;
import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.exception.InvalidScreenException;
import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreenTest {

    private static final ScreenType TWO_D = new ScreenType(UUID.randomUUID(), "2D", null);
    private static final SeatType STANDARD = SeatType.builder().id(UUID.randomUUID()).name("STANDARD")
            .priceMultiplier(BigDecimal.ONE).build();
    private static final SeatType VIP = SeatType.builder().id(UUID.randomUUID()).name("VIP")
            .priceMultiplier(new BigDecimal("1.50")).build();
    private static final Map<UUID, SeatType> TYPES = Map.of(STANDARD.getId(), STANDARD, VIP.getId(), VIP);

    @Test
    void layoutRowsBecomeLabelledSeats() {
        Screen screen = Screen.create(UUID.randomUUID(), " Room 1 ", TWO_D);

        List<Seat> seats = screen.buildSeats(List.of(
                new SeatRow("a", 3, STANDARD.getId()),
                new SeatRow("B", 2, VIP.getId())), TYPES);

        assertThat(screen.getName()).isEqualTo("Room 1");
        assertThat(screen.getTotalSeats()).isEqualTo(5);
        assertThat(seats).extracting(Seat::getLabel).containsExactly("A1", "A2", "A3", "B1", "B2");
        assertThat(seats.get(3).getPriceMultiplier()).isEqualByComparingTo("1.50");
    }

    @Test
    void duplicateRowsUnknownTypesAndBadLabelsAreRejected() {
        Screen screen = Screen.create(UUID.randomUUID(), "R", TWO_D);
        assertThatThrownBy(() -> screen.buildSeats(List.of(
                new SeatRow("A", 2, STANDARD.getId()), new SeatRow("A", 2, STANDARD.getId())), TYPES))
                .isInstanceOf(InvalidScreenException.class);
        assertThatThrownBy(() -> screen.buildSeats(List.of(new SeatRow("A", 2, UUID.randomUUID())), TYPES))
                .isInstanceOf(InvalidScreenException.class);
        assertThatThrownBy(() -> new SeatRow("A1", 2, STANDARD.getId())).isInstanceOf(InvalidScreenException.class);
        assertThatThrownBy(() -> new SeatRow("A", 51, STANDARD.getId())).isInstanceOf(InvalidScreenException.class);
    }

    @Test
    void layoutOfAnActiveScreenIsLocked() {
        Screen screen = Screen.create(UUID.randomUUID(), "R", TWO_D);
        assertThatThrownBy(screen::assertLayoutEditable).isInstanceOf(InvalidScreenException.class);

        screen.changeStatus(ScreenStatus.MAINTENANCE);
        screen.assertLayoutEditable();
    }
}
