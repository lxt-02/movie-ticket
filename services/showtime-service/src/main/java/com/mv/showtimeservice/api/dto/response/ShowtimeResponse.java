package com.mv.showtimeservice.api.dto.response;

import com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeResponse {
    private UUID id;
    private UUID movieId;
    private UUID screenId;
    private String movieTitle;
    private UUID cinemaId;
    private String cinemaName;
    private String screenName;
    private Instant startTime;
    private Instant endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Instant createdAt;
}
