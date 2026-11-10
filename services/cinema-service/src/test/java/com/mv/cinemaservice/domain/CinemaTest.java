package com.mv.cinemaservice.domain;

import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import com.mv.cinemaservice.domain.model.cinema.exception.InvalidCinemaException;
import com.mv.cinemaservice.domain.model.cinema.valueobject.CinemaDetails;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CinemaTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    private static CinemaDetails details(String name) {
        return new CinemaDetails(name, "CGV", "123 Le Loi", "Ho Chi Minh", "District 1", "028 3822 0000");
    }

    @Test
    void newCinemaIsActiveByDefault() {
        Cinema cinema = Cinema.create(details("CGV Vincom"), null, NOW);
        assertThat(cinema.getStatus()).isEqualTo(CinemaStatus.ACTIVE);
        assertThat(cinema.getId()).isNotNull();
    }

    @Test
    void requiredFieldsAndPhoneFormatAreChecked() {
        assertThatThrownBy(() -> details(" ")).isInstanceOf(InvalidCinemaException.class);
        assertThatThrownBy(() -> new CinemaDetails("X", null, " ", "HCM", null, null))
                .isInstanceOf(InvalidCinemaException.class);
        assertThatThrownBy(() -> new CinemaDetails("X", null, "addr", "HCM", null, "call-me!"))
                .isInstanceOf(InvalidCinemaException.class);
    }

    @Test
    void activeAndInactiveCanSwitchButClosedIsFinal() {
        Cinema cinema = Cinema.create(details("X"), null, NOW);
        cinema.changeStatus(CinemaStatus.INACTIVE);
        cinema.changeStatus(CinemaStatus.ACTIVE);
        cinema.close();
        cinema.close(); // idempotent

        assertThat(cinema.isClosed()).isTrue();
        assertThatThrownBy(() -> cinema.changeStatus(CinemaStatus.ACTIVE)).isInstanceOf(InvalidCinemaException.class);
        assertThatThrownBy(() -> cinema.update(details("Y"))).isInstanceOf(InvalidCinemaException.class);
    }

    @Test
    void newCinemaCannotBeClosed() {
        assertThatThrownBy(() -> Cinema.create(details("X"), CinemaStatus.CLOSED, NOW))
                .isInstanceOf(InvalidCinemaException.class);
    }
}
