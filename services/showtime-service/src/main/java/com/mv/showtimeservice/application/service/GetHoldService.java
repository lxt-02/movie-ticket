package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.port.in.GetHoldUseCase;
import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.query.GetHoldByBookingIdQuery;
import com.mv.showtimeservice.application.query.GetHoldByIdQuery;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetHoldService implements GetHoldUseCase {

    private final LoadSeatHoldPort loadSeatHoldPort;

    @Override
    public SeatHold getById(GetHoldByIdQuery query) {
        return loadSeatHoldPort.findById(query.getHoldId())
                .orElseThrow(() -> new SeatHoldNotFoundException(query.getHoldId()));
    }

    @Override
    public SeatHold getByBookingId(GetHoldByBookingIdQuery query) {
        return loadSeatHoldPort.findByBookingId(query.getBookingId())
                .orElseThrow(() -> SeatHoldNotFoundException.byBookingId(query.getBookingId()));
    }
}
