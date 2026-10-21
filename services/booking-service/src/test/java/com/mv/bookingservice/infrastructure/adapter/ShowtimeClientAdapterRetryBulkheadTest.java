package com.mv.bookingservice.infrastructure.adapter;

import com.mv.bookingservice.application.exception.DependencyUnavailableException;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.exception.BookingConflictException;
import com.mv.bookingservice.infrastructure.client.ShowtimeFeignClient;
import com.mv.bookingservice.infrastructure.resilience.BulkheadProperties;
import com.mv.bookingservice.infrastructure.resilience.CircuitBreakerProperties;
import com.mv.bookingservice.infrastructure.resilience.ResilienceConfig;
import com.mv.bookingservice.infrastructure.resilience.RetryProperties;
import com.mv.bookingservice.infrastructure.client.dto.RemoteResponse;
import com.mv.bookingservice.infrastructure.client.dto.SeatHoldResponse;
import feign.FeignException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Retry → CircuitBreaker → Bulkhead around the showtime calls of the booking flow. */
@ExtendWith(MockitoExtension.class)
class ShowtimeClientAdapterRetryBulkheadTest {

    @Mock
    private ShowtimeFeignClient feignClient;

    private ShowtimeClientAdapter adapter;
    private CircuitBreaker breaker;
    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        ResilienceConfig config = new ResilienceConfig();
        CircuitBreakerRegistry breakers = config.circuitBreakerRegistry(new CircuitBreakerProperties(10, 5, 50f, 20L, 3));
        RetryRegistry retries = config.retryRegistry(new RetryProperties(3, 1L, 1.0)); // 3 attempts, 1ms wait
        BulkheadRegistry bulkheads = config.bulkheadRegistry(new BulkheadProperties(1, 0L)); // 1 slot, no waiting
        adapter = new ShowtimeClientAdapter(feignClient, breakers, retries, bulkheads);
        breaker = breakers.circuitBreaker(ResilienceConfig.SHOWTIME);
        bulkhead = bulkheads.bulkhead(ResilienceConfig.SHOWTIME);
    }

    private static FeignException feignError(int status) {
        FeignException e = mock(FeignException.class);
        when(e.status()).thenReturn(status);
        return e;
    }

    private static RemoteResponse<SeatHoldResponse> released(UUID holdId) {
        SeatHoldResponse data = new SeatHoldResponse();
        data.setHoldId(holdId);
        data.setStatus("RELEASED");
        return RemoteResponse.success(data);
    }

    @Test
    void shortOutageIsRetriedAndTheUserNeverSeesIt() {
        UUID holdId = UUID.randomUUID();
        FeignException unavailable = feignError(503);
        when(feignClient.releaseHold(holdId)).thenThrow(unavailable).thenThrow(unavailable).thenReturn(released(holdId));

        ShowtimeClientPort.SeatHoldResult result = adapter.releaseHold(holdId);

        assertEquals(holdId, result.holdId());
        verify(feignClient, times(3)).releaseHold(holdId);
        // every attempt goes through the breaker: 2 failures + 1 success
        assertEquals(2, breaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    void stillDownAfterAllAttemptsBecomes503() {
        UUID holdId = UUID.randomUUID();
        FeignException unavailable = feignError(503);
        when(feignClient.releaseHold(holdId)).thenThrow(unavailable);

        assertThrows(DependencyUnavailableException.class, () -> adapter.releaseHold(holdId));
        verify(feignClient, times(3)).releaseHold(holdId);
    }

    @Test
    void readTimeoutIsNotRetried() {
        UUID holdId = UUID.randomUUID();
        FeignException timeout = feignError(-1);
        when(timeout.getCause()).thenReturn(new SocketTimeoutException("Read timed out"));
        when(feignClient.confirmHold(holdId)).thenThrow(timeout);

        assertThrows(DependencyUnavailableException.class, () -> adapter.confirmHold(holdId));
        verify(feignClient, times(1)).confirmHold(holdId); // slow service: do not add load
        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    void seatTakenIsNeverRetried() {
        FeignException conflict = feignError(409);
        when(feignClient.holdSeats(any(), any())).thenThrow(conflict);

        assertThrows(BookingConflictException.class,
                () -> adapter.holdSeats(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID())));
        verify(feignClient, times(1)).holdSeats(any(), any());
    }

    @Test
    void openBreakerIsNotRetriedAndShowtimeIsNotCalled() {
        breaker.transitionToOpenState();

        assertThrows(DependencyUnavailableException.class, () -> adapter.releaseHold(UUID.randomUUID()));
        verify(feignClient, never()).releaseHold(any());
    }

    @Test
    void fullBulkheadFailsFastWithoutCallingAndWithoutTouchingTheBreaker() {
        assertTrue(bulkhead.tryAcquirePermission()); // the only slot is busy (a slow call in progress)
        try {
            DependencyUnavailableException ex = assertThrows(DependencyUnavailableException.class,
                    () -> adapter.releaseHold(UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("quá tải"));
            verify(feignClient, never()).releaseHold(any());
            assertEquals(0, breaker.getMetrics().getNumberOfBufferedCalls());
        } finally {
            bulkhead.releasePermission();
        }
    }
}
