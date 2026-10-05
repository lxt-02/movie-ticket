package com.mv.bookingservice.infrastructure.persistence.mapper;

import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.entity.BookingSeat;
import com.mv.bookingservice.domain.model.outbox.OutboxEvent;
import com.mv.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.mv.bookingservice.infrastructure.persistence.entity.BookingSeatEntity;
import com.mv.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingPersistenceMapper {

    public Booking toDomain(BookingEntity entity) {
        if (entity == null) {
            return null;
        }

        List<BookingSeat> seats = new ArrayList<>();
        if (entity.getSeats() != null) {
            seats = entity.getSeats().stream()
                    .map(this::toSeatDomain)
                    .collect(Collectors.toList());
        }

        return Booking.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .userId(entity.getUserId())
                .showtimeId(entity.getShowtimeId())
                .movieTitle(entity.getMovieTitle())
                .cinemaName(entity.getCinemaName())
                .screenName(entity.getScreenName())
                .startTime(entity.getStartTime())
                .subtotal(entity.getSubtotal())
                .discountAmount(entity.getDiscountAmount())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .confirmedAt(entity.getConfirmedAt())
                .cancelledAt(entity.getCancelledAt())
                .holdId(entity.getHoldId())
                .idempotencyKey(entity.getIdempotencyKey())
                .requestHash(entity.getRequestHash())
                .currency(entity.getCurrency())
                .paidPaymentId(entity.getPaidPaymentId())
                .version(entity.getVersion())
                .updatedAt(entity.getUpdatedAt())
                .seats(seats)
                .build();
    }

    public BookingEntity toEntity(Booking domain) {
        if (domain == null) {
            return null;
        }

        BookingEntity entity = BookingEntity.builder()
                .id(domain.getId())
                .bookingCode(domain.getBookingCode())
                .userId(domain.getUserId())
                .showtimeId(domain.getShowtimeId())
                .movieTitle(domain.getMovieTitle())
                .cinemaName(domain.getCinemaName())
                .screenName(domain.getScreenName())
                .startTime(domain.getStartTime())
                .subtotal(domain.getSubtotal())
                .discountAmount(domain.getDiscountAmount())
                .totalAmount(domain.getTotalAmount())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .confirmedAt(domain.getConfirmedAt())
                .cancelledAt(domain.getCancelledAt())
                .holdId(domain.getHoldId())
                .idempotencyKey(domain.getIdempotencyKey())
                .requestHash(domain.getRequestHash())
                .currency(domain.getCurrency())
                .paidPaymentId(domain.getPaidPaymentId())
                .version(domain.getVersion())
                .updatedAt(domain.getUpdatedAt())
                .seats(new ArrayList<>())
                .build();

        if (domain.getSeats() != null) {
            for (BookingSeat seatDomain : domain.getSeats()) {
                BookingSeatEntity seatEntity = toSeatEntity(seatDomain, entity);
                entity.addSeat(seatEntity);
            }
        }

        return entity;
    }

    public BookingSeat toSeatDomain(BookingSeatEntity entity) {
        if (entity == null) {
            return null;
        }
        return BookingSeat.builder()
                .id(entity.getId())
                .bookingId(entity.getBooking() != null ? entity.getBooking().getId() : null)
                .showtimeSeatId(entity.getShowtimeSeatId())
                .seatId(entity.getSeatId())
                .seatLabel(entity.getSeatLabel())
                .unitPrice(entity.getUnitPrice())
                .ticketCode(entity.getTicketCode())
                .status(entity.getStatus())
                .build();
    }

    public BookingSeatEntity toSeatEntity(BookingSeat domain, BookingEntity parent) {
        if (domain == null) {
            return null;
        }
        return BookingSeatEntity.builder()
                .id(domain.getId())
                .booking(parent)
                .showtimeSeatId(domain.getShowtimeSeatId())
                .seatId(domain.getSeatId())
                .seatLabel(domain.getSeatLabel())
                .unitPrice(domain.getUnitPrice())
                .ticketCode(domain.getTicketCode())
                .status(domain.getStatus())
                .build();
    }

    public OutboxEvent toOutboxDomain(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return OutboxEvent.builder()
                .id(entity.getId())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public OutboxEventEntity toOutboxEntity(OutboxEvent domain) {
        if (domain == null) {
            return null;
        }
        return OutboxEventEntity.builder()
                .id(domain.getId())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .publishedAt(domain.getPublishedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
