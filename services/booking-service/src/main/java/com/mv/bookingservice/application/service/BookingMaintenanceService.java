package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.dto.BookingPaymentPolicy;
import com.mv.bookingservice.application.port.in.BookingMaintenanceUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.PaymentCommandPort;
import com.mv.bookingservice.application.port.out.PaymentStatusPort;
import com.mv.bookingservice.application.port.out.PaymentStatusPort.PaidPayment;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Background jobs of booking-service.
 * <ul>
 *     <li>Expiry: PENDING bookings past their deadline (+ grace) are expired and their seats released. Before that,
 *     payment-service is asked directly whether the booking is in fact paid: a PAYMENT_SUCCEEDED event that was
 *     lost or is still parked in the dead-letter topic must not cost a buyer the seats they paid for.</li>
 *     <li>Seat confirmation retry for CONFIRMING bookings (Showtime was down when the payment arrived).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
// [VI] Các job nền của booking-service:
//  - expireOverdueBookings(): LƯỚI AN TOÀN cho hết hạn. Bình thường payment-service quyết định hết hạn
//    (nó biết trạng thái tiền trên PayOS) và gửi PAYMENT_EXPIRED. Job này chạy trễ hơn 90 giây (grace)
//    và chỉ bắt các booking mà PAYMENT_EXPIRED chưa tới (payment sập, event kẹt DLT...).
//  - retrySeatConfirmations(): thử chốt ghế lại cho booking CONFIRMING (lúc tiền tới thì Showtime đang lỗi).
public class BookingMaintenanceService implements BookingMaintenanceUseCase {

    static final String EXPIRED_REASON = "Booking expired";

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final PaymentCommandPort paymentCommandPort;
    private final SeatConfirmation seatConfirmation;
    private final BookingPaymentPolicy policy;
    private final PaymentStatusPort paymentStatusPort;

    @Override
    @Transactional
    // [VI] Lấy booking PENDING đã quá expiresAt + grace (khoá FOR UPDATE). Với từng booking:
    //  1. Hỏi payment-service qua HTTP (GET /internal/payments/by-booking/{id}/paid-status, hợp đồng paid-status.json)
    //     xem thực ra đã trả tiền chưa -> tránh làm mất vé của khách khi PAYMENT_SUCCEEDED bị lạc.
    //  2. Đã trả hợp lệ -> recoverPaidBooking() xác nhận thay vì expire.
    //  3. Chưa trả -> expire, gửi CANCEL_PAYMENT qua outbox để đóng link PayOS còn mở, rồi nhả ghế.
    public int expireOverdueBookings() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(policy.expiryGrace());
        List<Booking> overdue = loadBookingPort.findOverduePendingForUpdate(cutoff, policy.batchSize());
        int expired = 0;
        for (Booking booking : overdue) {
            Optional<PaidPayment> paid;
            try {
                paid = paymentStatusPort.findSucceededPayment(booking.getId());
            } catch (RuntimeException e) {
                // [VI] payment-service không trả lời: nếu hold ghế còn thì hoãn sang lượt sau (không đoán);
                // nếu hold đã hết thì expire luôn, vì ghế không còn để xác nhận.
                if (holdStillAlive(booking, now)) {
                    // Do not guess: the seats are still held, so wait for the next run.
                    log.warn("Cannot ask payment-service about booking {} ({}); expiry postponed to the next run",
                            booking.getId(), e.getMessage());
                    continue;
                }
                // The seat hold is over anyway: expire. A payment found later is logged for manual check.
                log.warn("payment-service unreachable for booking {} and its seat hold is over: expiring it ({})",
                        booking.getId(), e.getMessage());
                paid = Optional.empty();
            }
            if (paid.isPresent() && recoverPaidBooking(booking, paid.get())) {
                continue;
            }
            booking.expire();
            Booking saved = saveBookingPort.save(booking);
            // Close any PayOS link still open so the buyer cannot pay for released seats.
            // [VI] Ghi CANCEL_PAYMENT vào outbox cùng transaction; BookingOutboxRelay sẽ gửi lên topic payment-commands.
            paymentCommandPort.cancelPayments(saved, EXPIRED_REASON);
            releaseHoldQuietly(saved);
            expired++;
            log.info("Booking {} EXPIRED (expires_at {})", saved.getId(), saved.getExpiresAt());
        }
        return expired;
    }

    /** The Showtime seat hold ends {@code holdSafetyMargin} after the payment deadline. */
    // [VI] Hold ghế ở Showtime hết sau expiresAt + holdSafetyMargin (mặc định 180 s).
    private boolean holdStillAlive(Booking booking, Instant now) {
        return booking.getExpiresAt() != null && now.isBefore(booking.getExpiresAt().plus(policy.holdSafetyMargin()));
    }

    // [VI] Booking đã được trả nhưng event SUCCEEDED chưa áp dụng: kiểm tra đúng tiền + đúng hạn rồi xác nhận ghế.
    // Sai tiền hoặc trả trễ -> trả false để booking bị expire và log MANUAL CHECK.
    private boolean recoverPaidBooking(Booking booking, PaidPayment payment) {
        boolean sameAmount = payment.amount() != null && booking.getTotalAmount() != null
                && payment.amount().compareTo(booking.getTotalAmount()) == 0
                && payment.currency() != null && payment.currency().equalsIgnoreCase(booking.getCurrency());
        if (!sameAmount || !booking.wasPaidInTime(payment.paidAt())) {
            String reason = "Payment " + payment.paymentId() + " (" + payment.amount() + " " + payment.currency()
                    + ", paid at " + payment.paidAt() + ") cannot be used: booking total " + booking.getTotalAmount()
                    + " " + booking.getCurrency() + ", deadline " + booking.getExpiresAt();
            log.error("MANUAL CHECK: booking {}: {}", booking.getId(), reason);
            return false;
        }
        log.warn("SAFETY NET: booking {} is PAID by {} but PAYMENT_SUCCEEDED was never applied; confirming it "
                + "instead of expiring it", booking.getId(), payment.paymentId());
        booking.markConfirming(payment.paymentId(), payment.paidAt());
        // CONFIRMED, CANCELLED (seats lost, flagged) or still CONFIRMING (Showtime down: the retry job goes on).
        seatConfirmation.tryConfirm(booking);
        saveBookingPort.save(booking);
        return true;
    }

    @Override
    @Transactional
    // [VI] Booking CONFIRMING = đã có tiền nhưng chưa chốt được ghế ở Showtime. Thử lại tới khi CONFIRMED
    // hoặc CANCELLED (ghế đã mất).
    public int retrySeatConfirmations() {
        int confirmed = 0;
        for (Booking booking : loadBookingPort.findConfirmingForUpdate(policy.batchSize())) {
            SeatConfirmation.Outcome outcome = seatConfirmation.tryConfirm(booking);
            if (outcome.changed()) {
                // CONFIRMED, or CANCELLED (seats lost): either way it leaves the CONFIRMING retry queue.
                saveBookingPort.save(booking);
            }
            if (outcome == SeatConfirmation.Outcome.CONFIRMED) {
                confirmed++;
            }
        }
        return confirmed;
    }

    private void releaseHoldQuietly(Booking booking) {
        AfterCommit.run(() -> releaseHoldNow(booking));
    }

    private void releaseHoldNow(Booking booking) {
        if (booking.getHoldId() == null) {
            return;
        }
        try {
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            // Showtime expires the hold on its own.
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
    }
}
