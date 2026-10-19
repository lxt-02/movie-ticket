package com.mv.showtimeservice.infrastructure.config;

import com.mv.showtimeservice.application.dto.ShowtimeSchedulePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ShowtimeConfig {

    @Bean
    public ShowtimeSchedulePolicy showtimeSchedulePolicy(
            @Value("${showtime.schedule.cleaning-buffer-minutes:15}") long cleaningBufferMinutes) {
        return new ShowtimeSchedulePolicy(Duration.ofMinutes(Math.max(0, cleaningBufferMinutes)));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
