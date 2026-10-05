package com.mv.cinemaservice.application.port.in;

import com.mv.cinemaservice.application.query.GetScreenLayoutQuery;
import com.mv.cinemaservice.application.query.GetScreenQuery;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;

public interface GetScreenUseCase {
    Screen getById(GetScreenQuery query);

    ScreenLayoutResult getLayout(GetScreenLayoutQuery query);
}
