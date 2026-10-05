package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.application.port.out.LoadSeatPort;
import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetScreenServiceTest {

    @Mock
    private LoadScreenPort loadScreenPort;

    @Mock
    private LoadSeatPort loadSeatPort;

    private GetScreenService getScreenService;

    @BeforeEach
    void setUp() {
        getScreenService = new GetScreenService(loadScreenPort, loadSeatPort);
    }

    @Test
    void shouldReturnScreenLayout() {
        UUID screenId = UUID.randomUUID();
        Screen screen = Screen.builder()
                .id(screenId)
                .cinemaId(UUID.randomUUID())
                .name("Screen 1")
                .totalSeats(100)
                .status(ScreenStatus.ACTIVE)
                .build();

        Seat seat1 = Seat.builder()
                .id(UUID.randomUUID())
                .screenId(screenId)
                .rowLabel("A")
                .seatNumber(1)
                .label("A1")
                .priceMultiplier(BigDecimal.ONE)
                .status(SeatStatus.ACTIVE)
                .build();

        when(loadScreenPort.findById(screenId)).thenReturn(Optional.of(screen));
        when(loadSeatPort.findByScreenId(screenId)).thenReturn(List.of(seat1));

        ScreenLayoutResult result = getScreenService.getLayout(new GetScreenLayoutQuery(screenId));

        assertNotNull(result);
        assertEquals(screenId, result.getScreen().getId());
        assertEquals(1, result.getSeats().size());
        assertEquals("A1", result.getSeats().get(0).getLabel());
    }
}
