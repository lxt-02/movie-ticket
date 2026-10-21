package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.port.in.ShowtimeLifecycleUseCase;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Showtime status over time:
 * <pre>
 * SCHEDULED --(start - sales-open-days)--> SELLING --(end time passed)--> ENDED
 * </pre>
 * Both SCHEDULED and SELLING accept seat holds until the start (unchanged rule); SELLING tells the frontend the
 * showtime is in its sales window. A SCHEDULED showtime that was never opened also ends after its end time.
 * Bulk updates: no row-by-row locking needed, a showtime past its start cannot be held anyway.
 */
@Slf4j
@Service
public class ShowtimeLifecycleService implements ShowtimeLifecycleUseCase {

    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final Clock clock;
    private final Duration salesWindow;

    public ShowtimeLifecycleService(ShowtimeRepositoryPort showtimeRepositoryPort, Clock clock,
                                    @Value("${showtime.lifecycle.sales-open-days:7}") long salesOpenDays) {
        this.showtimeRepositoryPort = showtimeRepositoryPort;
        this.clock = clock;
        this.salesWindow = Duration.ofDays(Math.max(0, salesOpenDays));
    }

    @Override
    @Transactional
    public int advance() {
        Instant now = clock.instant();
        int opened = showtimeRepositoryPort.openSales(now.plus(salesWindow), now);
        int ended = showtimeRepositoryPort.endFinished(now);
        if (opened + ended > 0) {
            log.info("Showtime lifecycle: {} opened for sale, {} ended", opened, ended);
        }
        return opened + ended;
    }
}
