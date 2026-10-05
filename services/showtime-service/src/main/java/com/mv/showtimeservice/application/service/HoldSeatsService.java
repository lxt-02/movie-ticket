package com.mv.showtimeservice.application.service;

import com.mv.showtimeservice.application.command.HoldSeatsCommand;
import com.mv.showtimeservice.application.port.in.HoldSeatsResult;
import com.mv.showtimeservice.application.port.in.HoldSeatsUseCase;
import com.mv.showtimeservice.application.port.out.LoadSeatHoldPort;
import com.mv.showtimeservice.application.port.out.SaveSeatHoldPort;
import com.mv.showtimeservice.application.port.out.ShowtimeRepositoryPort;
import com.mv.showtimeservice.application.port.out.ShowtimeSeatRepositoryPort;
import com.mv.showtimeservice.domain.model.seathold.aggregate.SeatHold;
import com.mv.showtimeservice.domain.model.seathold.entity.SeatHoldItem;
import com.mv.showtimeservice.domain.model.seathold.enums.SeatHoldStatus;
import com.mv.showtimeservice.domain.model.showtime.aggregate.Showtime;
import com.mv.showtimeservice.domain.model.showtime.entity.ShowtimeSeat;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatHoldConflictException;
import com.mv.showtimeservice.domain.model.seathold.exception.SeatUnavailableException;
import com.mv.showtimeservice.domain.model.showtime.exception.ShowtimeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.mv.showtimeservice.domain.model.showtime.enums.ShowtimeSeatStatus.HELD;

@Service
@RequiredArgsConstructor
public class HoldSeatsService implements HoldSeatsUseCase {

    private final ShowtimeRepositoryPort showtimeRepositoryPort;
    private final ShowtimeSeatRepositoryPort showtimeSeatRepositoryPort;
    private final SaveSeatHoldPort saveSeatHoldPort;
    private final LoadSeatHoldPort loadSeatHoldPort;

    @Override
    @Transactional
    public HoldSeatsResult execute(HoldSeatsCommand command) {
        // 1. Idempotency check:
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
            Optional<SeatHold> existingHoldOpt = loadSeatHoldPort.findByIdempotencyKey(command.getIdempotencyKey());
            if (existingHoldOpt.isPresent()) {
                SeatHold existingHold = existingHoldOpt.get();
                if (command.getRequestHash() != null && command.getRequestHash().equals(existingHold.getRequestHash())) {
                    Showtime showtime = showtimeRepositoryPort.findById(existingHold.getShowtimeId())
                            .orElseThrow(() -> new ShowtimeNotFoundException(existingHold.getShowtimeId()));
                    List<ShowtimeSeat> seats = showtimeSeatRepositoryPort.findByHoldId(existingHold.getId());
                    return new HoldSeatsResult(existingHold, showtime, seats);
                } else {
                    throw new SeatHoldConflictException("Idempotency key reused with different request payload");
                }
            }
        }

        // Check if bookingId is already associated with another hold
        Optional<SeatHold> bookingHoldOpt = loadSeatHoldPort.findByBookingId(command.getBookingId());
        if (bookingHoldOpt.isPresent()) {
            throw new SeatHoldConflictException("Booking ID " + command.getBookingId() + " already has an existing seat hold");
        }

        // 2. Lock showtime row
        Showtime showtime = showtimeRepositoryPort.findByIdWithLock(command.getShowtimeId())
                .orElseThrow(() -> new ShowtimeNotFoundException(command.getShowtimeId()));

        if (!showtime.isSelling()) {
            throw new SeatUnavailableException("Showtime is not currently available for seat reservation");
        }

        // 3. Lock seats in fixed ascending ID order (prevent deadlock)
        List<UUID> sortedSeatIds = command.getShowtimeSeatIds().stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        if (sortedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one seat to hold");
        }

        List<ShowtimeSeat> lockedSeats = showtimeSeatRepositoryPort.findByIdsWithLock(sortedSeatIds);

        if (lockedSeats.size() != sortedSeatIds.size()) {
            throw new SeatUnavailableException("One or more requested seats could not be found");
        }

        // 4. Validate all seats belong to showtime and are AVAILABLE
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(command.getHoldDurationSeconds());

        List<SeatHoldItem> holdItems = new ArrayList<>();

        for (ShowtimeSeat seat : lockedSeats) {
            if (!seat.getShowtimeId().equals(command.getShowtimeId())) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " does not belong to showtime " + command.getShowtimeId());
            }

            if (seat.getStatus() == HELD && seat.getLockedUntil() != null && now.isAfter(seat.getLockedUntil())) {
                expireExistingHold(seat.getHoldId());
                seat.release();
            }

            if (!seat.isAvailable()) {
                throw new SeatUnavailableException("Seat " + seat.getSeatLabel() + " is already taken or locked by another transaction");
            }

            holdItems.add(SeatHoldItem.builder()
                    .showtimeSeatId(seat.getId())
                    .unitPrice(seat.getPrice())
                    .build());
        }

        // 5. Create SeatHold aggregate
        SeatHold seatHold = SeatHold.create(
                command.getBookingId(),
                command.getShowtimeId(),
                command.getIdempotencyKey(),
                command.getRequestHash(),
                expiresAt,
                holdItems
        );

        // 6. Update seats to HELD with hold_id and locked_until
        for (ShowtimeSeat seat : lockedSeats) {
            seat.hold(seatHold.getId(), expiresAt);
        }

        // 7. Save hold and seats atomically
        SeatHold savedHold = saveSeatHoldPort.save(seatHold);
        List<ShowtimeSeat> savedSeats = showtimeSeatRepositoryPort.saveAll(lockedSeats);

        return new HoldSeatsResult(savedHold, showtime, savedSeats);
    }

    private void expireExistingHold(UUID holdId) {
        if (holdId == null) {
            return;
        }

        loadSeatHoldPort.findById(holdId)
                .filter(hold -> hold.getStatus() == SeatHoldStatus.HELD)
                .filter(SeatHold::isExpired)
                .ifPresent(hold -> {
                    hold.expire();
                    List<ShowtimeSeat> heldSeats = showtimeSeatRepositoryPort.findByHoldId(hold.getId());
                    for (ShowtimeSeat heldSeat : heldSeats) {
                        if (heldSeat.getStatus() == HELD) {
                            heldSeat.release();
                        }
                    }
                    showtimeSeatRepositoryPort.saveAll(heldSeats);
                    saveSeatHoldPort.save(hold);
                });
    }
}
