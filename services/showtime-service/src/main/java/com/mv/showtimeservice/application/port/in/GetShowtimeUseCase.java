package com.mv.showtimeservice.application.port.in;

import com.mv.showtimeservice.application.dto.PageResult;
import com.mv.showtimeservice.application.query.SearchShowtimesQuery;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;

import java.util.List;
import java.util.UUID;

public interface GetShowtimeUseCase {
    Showtime getById(UUID showtimeId);

    PageResult<Showtime> search(SearchShowtimesQuery query);

    /** Seat map of a showtime, ordered by seat label. */
    List<ShowtimeSeat> getSeats(UUID showtimeId);
}
