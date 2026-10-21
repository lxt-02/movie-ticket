package com.mv.showtimeservice.infrastructure.scheduler;

import com.mv.showtimeservice.application.port.in.ShowtimeLifecycleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowtimeLifecycleScheduler {

    private final ShowtimeLifecycleUseCase lifecycleUseCase;

    @Scheduled(fixedDelayString = "${showtime.lifecycle.delay-ms:60000}", initialDelayString = "15000")
    public void advance() {
        try {
            lifecycleUseCase.advance();
        } catch (RuntimeException e) {
            log.error("Showtime lifecycle run failed", e);
        }
    }
}
