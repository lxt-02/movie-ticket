package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.port.in.GetScreenUseCase;
import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.application.port.out.LoadSeatPort;
import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.application.query.GetScreenQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.exception.ScreenNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetScreenService implements GetScreenUseCase {

    private final LoadScreenPort loadScreenPort;
    private final LoadSeatPort loadSeatPort;

    @Override
    public Screen getById(GetScreenQuery query) {
        return loadScreenPort.findById(query.getScreenId())
                .orElseThrow(() -> new ScreenNotFoundException(query.getScreenId()));
    }

    @Override
    public ScreenLayoutResult getLayout(GetScreenLayoutQuery query) {
        Screen screen = getById(new GetScreenQuery(query.getScreenId()));
        List<Seat> seats = loadSeatPort.findByScreenId(query.getScreenId());
        return new ScreenLayoutResult(screen, seats);
    }
}
