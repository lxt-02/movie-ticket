package com.mv.showtimeservice.application.dto;

import java.time.Duration;

/**
 * @param cleaningBuffer minimum gap between two showtimes of the same screen (cleaning, ads, entry/exit)
 */
public record ShowtimeSchedulePolicy(Duration cleaningBuffer) {
    public ShowtimeSchedulePolicy {
        if (cleaningBuffer == null || cleaningBuffer.isNegative()) {
            cleaningBuffer = Duration.ZERO;
        }
    }
}
