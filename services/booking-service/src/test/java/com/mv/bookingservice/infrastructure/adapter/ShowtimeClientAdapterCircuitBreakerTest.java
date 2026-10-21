package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import com.mv.bookingservice.application.exception.SeatHoldRejectedException;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.infrastructure.client.ShowtimeFeignClient;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import com.mv.bookingservice.infrastructure.resilience.CircuitBreakerProperties;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeClientAdapterCircuitBreakerTest {

    @Mock
    private ShowtimeFeignClient feignClient;

    private ShowtimeClientAdapter adapter;
    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        // window 10, min 5 calls, 50% failures -> OPEN, 20s open, 3 half-open calls
        CircuitBreakerRegistry registry = new ResilienceConfig()
                .circuitBreakerRegistry(new CircuitBreakerProperties(10, 5, 50f, 20L, 3));
        adapter = new ShowtimeClientAdapter(feignClient, registry);
        breaker = registry.circuitBreaker(ResilienceConfig.SHOWTIME);
    }

    private static FeignException feignError(int status) {
        FeignException e = mock(FeignException.class);
        when(e.status()).thenReturn(status);
        return e;
    }

    @Test
    void opensAfterRepeatedTechnicalFailuresThenRejectsWithoutCallingShowtime() {
        UUID holdId = UUID.randomUUID();
        FeignException down = feignError(503);
        when(feignClient.releaseHold(holdId)).thenThrow(down);

        for (int i = 0; i < 5; i++) {
            // the failure is recorded, then reported as 503 (DependencyUnavailableException)
            assertThrows(DependencyUnavailableException.class, () -> adapter.releaseHold(holdId));
        }
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        assertThrows(DependencyUnavailableException.class, () -> adapter.releaseHold(holdId));
        verify(feignClient, times(5)).releaseHold(holdId); // 6th call never reached showtime
    }

    @Test
    void businessConflictsNeverOpenTheBreaker() {
        UUID holdId = UUID.randomUUID();
        FeignException conflict = feignError(409);
        when(feignClient.confirmHold(holdId)).thenThrow(conflict);

        for (int i = 0; i < 10; i++) {
            assertThrows(SeatHoldRejectedException.class, () -> adapter.confirmHold(holdId));
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    void seatTakenWhileHoldingIsAConflictNotAFailure() {
        FeignException conflict = feignError(409);
        when(feignClient.holdSeats(any(), any())).thenThrow(conflict);

        for (int i = 0; i < 6; i++) {
            assertThrows(BookingConflictException.class,
                    () -> adapter.holdSeats(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID())));
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void closesAgainAfterSuccessfulTrialCallsInHalfOpen() {
        UUID holdId = UUID.randomUUID();
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        SeatHoldResponse released = new SeatHoldResponse();
        released.setHoldId(holdId);
        released.setStatus("RELEASED");
        RemoteResponse<SeatHoldResponse> ok = new RemoteResponse<>();
        ok.setSuccess(true);
        ok.setData(released);
        when(feignClient.releaseHold(holdId)).thenReturn(ok);

        for (int i = 0; i < 3; i++) {
            adapter.releaseHold(holdId);
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void onlyTechnicalFailuresAreRecorded() {
        assertTrue(ResilienceConfig.isTechnicalFailure(feignError(503)));
        assertTrue(ResilienceConfig.isTechnicalFailure(feignError(500)));
        assertTrue(ResilienceConfig.isTechnicalFailure(feignError(-1)));
        assertTrue(ResilienceConfig.isTechnicalFailure(new IllegalStateException("bad body")));
        assertFalse(ResilienceConfig.isTechnicalFailure(feignError(409)));
        assertFalse(ResilienceConfig.isTechnicalFailure(feignError(404)));
        assertFalse(ResilienceConfig.isTechnicalFailure(new SeatHoldRejectedException("gone")));
        assertFalse(ResilienceConfig.isTechnicalFailure(new BookingConflictException("taken")));
    }
}
