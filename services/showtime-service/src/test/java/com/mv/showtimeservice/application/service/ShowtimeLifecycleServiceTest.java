package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    @Mock
    private ShowtimeRepositoryPort showtimeRepositoryPort;

    @Test
    void opensSalesInsideTheWindowAndEndsFinishedShowtimes() {
        when(showtimeRepositoryPort.openSales(NOW.plus(Duration.ofDays(7)), NOW)).thenReturn(2);
        when(showtimeRepositoryPort.endFinished(NOW)).thenReturn(3);
        ShowtimeLifecycleService service =
                new ShowtimeLifecycleService(showtimeRepositoryPort, Clock.fixed(NOW, ZoneOffset.UTC), 7);

        assertEquals(5, service.advance());
        verify(showtimeRepositoryPort).openSales(NOW.plus(Duration.ofDays(7)), NOW);
    }
}
