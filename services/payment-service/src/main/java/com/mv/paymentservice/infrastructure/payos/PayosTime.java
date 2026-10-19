package com.mv.paymentservice.infrastructure.payos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses PayOS transaction times. Webhooks send "2023-02-04 18:25:00" (Vietnam local time); other endpoints may
 * send ISO-8601 with or without an offset. Unknown formats return null (callers then use the processing time).
 */
final class PayosTime {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PayosTime() {
    }

    static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return LocalDateTime.parse(text).atZone(VIETNAM).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return LocalDateTime.parse(text, SPACE_FORMAT).atZone(VIETNAM).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
