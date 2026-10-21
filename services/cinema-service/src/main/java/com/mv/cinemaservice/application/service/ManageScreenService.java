package com.mv.cinemaservice.application.service;

import com.mv.cinemaservice.application.command.SaveScreenCommand;
import com.mv.cinemaservice.application.port.in.GetReferenceDataUseCase;
import com.mv.cinemaservice.application.port.in.ManageScreenUseCase;
import com.mv.cinemaservice.application.port.in.ScreenLayoutResult;
import com.mv.cinemaservice.application.port.out.LoadCinemaPort;
import com.mv.cinemaservice.application.port.out.LoadReferenceDataPort;
import com.mv.cinemaservice.application.port.out.LoadScreenPort;
import com.mv.cinemaservice.application.port.out.LoadSeatPort;
import com.mv.cinemaservice.application.port.out.SaveScreenPort;
import com.mv.cinemaservice.application.port.out.SaveSeatPort;
import com.mv.cinemaservice.domain.model.cinema.aggregate.Cinema;
import com.mv.cinemaservice.domain.model.cinema.exception.CinemaNotFoundException;
import com.mv.cinemaservice.domain.model.cinema.exception.InvalidCinemaException;
import com.mv.cinemaservice.domain.model.screen.aggregate.Screen;
import com.mv.cinemaservice.domain.model.screen.entity.ScreenType;
import com.mv.cinemaservice.domain.model.screen.entity.Seat;
import com.mv.cinemaservice.domain.model.screen.entity.SeatType;
import com.mv.cinemaservice.domain.model.screen.enums.ScreenStatus;
import com.mv.cinemaservice.domain.model.screen.enums.SeatStatus;
import com.mv.cinemaservice.domain.model.screen.exception.InvalidScreenException;
import com.mv.cinemaservice.domain.model.screen.exception.ScreenConflictException;
import com.mv.cinemaservice.domain.model.screen.exception.ScreenNotFoundException;
import com.mv.cinemaservice.domain.model.screen.valueobject.SeatRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Screens and seat layouts. A screen belongs to a cinema that is not CLOSED; names are unique per cinema.
 * Showtimes copy the layout when they are created, so changes here only affect new showtimes.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ManageScreenService implements ManageScreenUseCase, GetReferenceDataUseCase {

    private final LoadCinemaPort loadCinemaPort;
    private final LoadScreenPort loadScreenPort;
    private final SaveScreenPort saveScreenPort;
    private final LoadSeatPort loadSeatPort;
    private final SaveSeatPort saveSeatPort;
    private final LoadReferenceDataPort referenceDataPort;

    @Override
    public ScreenLayoutResult create(UUID cinemaId, SaveScreenCommand command) {
        requireOpenCinema(cinemaId);
        Screen screen = Screen.create(cinemaId, command.getName(), screenType(command.getScreenTypeId()));
        refuseDuplicateName(cinemaId, screen.getName(), null);
        List<Seat> seats = screen.buildSeats(command.getRows(), seatTypesById());
        Screen saved = saveScreenPort.save(screen);
        return new ScreenLayoutResult(saved, saveSeatPort.replaceSeats(saved.getId(), seats));
    }

    @Override
    public Screen update(UUID screenId, SaveScreenCommand command) {
        Screen screen = load(screenId);
        requireOpenCinema(screen.getCinemaId());
        screen.update(command.getName(), screenType(command.getScreenTypeId()));
        refuseDuplicateName(screen.getCinemaId(), screen.getName(), screen.getId());
        return saveScreenPort.save(screen);
    }

    @Override
    public Screen changeStatus(UUID screenId, ScreenStatus status) {
        Screen screen = load(screenId);
        if (status == ScreenStatus.ACTIVE) {
            requireOpenCinema(screen.getCinemaId());
        }
        screen.changeStatus(status);
        return saveScreenPort.save(screen);
    }

    @Override
    public ScreenLayoutResult replaceLayout(UUID screenId, List<SeatRow> rows) {
        Screen screen = load(screenId);
        requireOpenCinema(screen.getCinemaId());
        screen.assertLayoutEditable();
        List<Seat> seats = screen.buildSeats(rows, seatTypesById());
        Screen saved = saveScreenPort.save(screen);
        return new ScreenLayoutResult(saved, saveSeatPort.replaceSeats(screenId, seats));
    }

    @Override
    public Seat changeSeatStatus(UUID screenId, UUID seatId, SeatStatus status) {
        load(screenId);
        Seat seat = loadSeatPort.findById(seatId)
                .filter(s -> screenId.equals(s.getScreenId()))
                .orElseThrow(() -> new InvalidScreenException("Ghế " + seatId + " không thuộc phòng " + screenId));
        seat.changeStatus(status);
        return saveSeatPort.save(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenType> screenTypes() {
        return referenceDataPort.findAllScreenTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatType> seatTypes() {
        return referenceDataPort.findAllSeatTypes();
    }

    private Screen load(UUID screenId) {
        return loadScreenPort.findById(screenId).orElseThrow(() -> new ScreenNotFoundException(screenId));
    }

    private void requireOpenCinema(UUID cinemaId) {
        Cinema cinema = loadCinemaPort.findById(cinemaId).orElseThrow(() -> new CinemaNotFoundException(cinemaId));
        if (cinema.isClosed()) {
            throw new InvalidCinemaException("Rạp " + cinemaId + " đã đóng cửa (CLOSED), không thể thay đổi phòng chiếu");
        }
    }

    private void refuseDuplicateName(UUID cinemaId, String name, UUID excludeScreenId) {
        if (saveScreenPort.existsByName(cinemaId, name, excludeScreenId)) {
            throw new ScreenConflictException("Rạp đã có phòng tên '" + name + "'");
        }
    }

    private ScreenType screenType(UUID id) {
        if (id == null) {
            throw new InvalidScreenException("Loại phòng là bắt buộc");
        }
        return referenceDataPort.findScreenType(id).orElse(null);
    }

    private Map<UUID, SeatType> seatTypesById() {
        return referenceDataPort.findAllSeatTypes().stream()
                .collect(Collectors.toMap(SeatType::getId, Function.identity()));
    }
}
