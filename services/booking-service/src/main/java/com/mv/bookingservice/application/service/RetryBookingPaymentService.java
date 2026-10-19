package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.RetryBookingPaymentCommand;
import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.in.RetryBookingPaymentUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.exception.BookingNotFoundException;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Pay again" for a PENDING booking: next attempt number, new CREATE_PAYMENT with a new idempotency key.
 * payment-service cancels the previous PayOS link before opening the new one (and refuses if the old one was paid).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryBookingPaymentService implements RetryBookingPaymentUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final PaymentCommandPort paymentCommandPort;
    private final BookingPaymentPolicy policy;

    @Override
    @Transactional
    public Booking execute(RetryBookingPaymentCommand command) {
        Booking booking = loadBookingPort.findById(command.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.getBookingId()));
        if (command.getUserId() == null || !command.getUserId().equals(booking.getUserId())) {
            throw new BookingValidationException("Access denied to pay this booking");
        }

        booking.startNewPaymentAttempt(policy.maxPaymentAttempts());
        Booking saved = saveBookingPort.save(booking);
        paymentCommandPort.requestPayment(saved);
        log.info("Booking {} payment attempt {} requested", saved.getId(), saved.currentPaymentAttempt());
        return saved;
    }
}
