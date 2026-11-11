package com.mv.bookingservice.application.service;

import com.mv.bookingservice.application.command.PaymentEventCommand;
import com.mv.bookingservice.application.port.in.HandlePaymentEventUseCase;
import com.mv.bookingservice.application.port.out.LoadBookingPort;
import com.mv.bookingservice.application.port.out.SaveBookingPort;
import com.mv.bookingservice.application.port.out.ShowtimeClientPort;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Applies payment-events to bookings. No inbox table: every handler is guarded by the booking state (PENDING,
 * CONFIRMING, CONFIRMED...), so a redelivered event finds the booking already changed and does nothing.
 * Business mismatches are logged and acknowledged (retrying cannot fix them); only technical
 * errors (database, Showtime call) are thrown so Kafka redelivers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// [VI] Nơi booking PHẢN ỨNG với 6 loại event từ payment-service (topic payment-events).
// Chỉ 3 event làm booking đổi trạng thái: PAYMENT_SUCCEEDED, PAYMENT_EXPIRED, PAYMENT_CREATE_FAILED
// (trừ khi lý do là PAYMENT_PROVIDER_ERROR). 3 event còn lại (LINK_CREATED, FAILED, CANCELLED) chỉ ghi log.
// Ba quy tắc chung:
//  1. Không có bảng inbox: mỗi nhánh tự kiểm tra trạng thái booking trước khi đổi, nên event bị Kafka
//     giao lại lần 2 sẽ thấy booking đã đổi rồi và không làm gì (idempotent theo trạng thái).
//  2. Lỗi nghiệp vụ (sai tiền, booking đã đóng...) chỉ log rồi ack, vì retry cũng không sửa được.
//     Chỉ lỗi kỹ thuật (DB, gọi Showtime) mới ném ra để Kafka retry, hết lượt thì vào payment-events.DLT.
//  3. Nhả ghế luôn qua releaseHoldQuietly() -> AfterCommit.run(): chỉ nhả sau khi transaction commit.
public class HandlePaymentEventService implements HandlePaymentEventUseCase {

    private final LoadBookingPort loadBookingPort;
    private final SaveBookingPort saveBookingPort;
    private final ShowtimeClientPort showtimeClientPort;
    private final SeatConfirmation seatConfirmation;

    @Override
    @Transactional
    // [VI] Điểm vào, được PaymentEventListener gọi sau khi parse JSON. Chạy trong 1 transaction:
    // thiếu eventId/bookingId thì bỏ qua; không tìm thấy booking thì chỉ log (không retry vô ích).
    public void handle(PaymentEventCommand event) {
        if (event.getEventId() == null || event.getBookingId() == null) {
            log.warn("Ignoring payment event without eventId/bookingId: {}", event.getEventType());
            return;
        }
        Optional<Booking> found = loadBookingPort.findById(event.getBookingId());
        if (found.isEmpty()) {
            log.warn("Payment event {} for unknown booking {}", event.getEventType(), event.getBookingId());
        } else {
            apply(found.get(), event);
        }
    }

    // [VI] Rẽ nhánh theo eventType (giá trị nằm trong contracts/booking-payment/contract.json).
    private void apply(Booking booking, PaymentEventCommand event) {
        String type = event.getEventType() == null ? "" : event.getEventType();
        switch (type) {
            case PaymentEventCommand.PAYMENT_SUCCEEDED -> onPaymentSucceeded(booking, event);
            case PaymentEventCommand.PAYMENT_CREATE_FAILED -> onPaymentCreateFailed(booking, event);
            case PaymentEventCommand.PAYMENT_EXPIRED -> onPaymentExpired(booking);
            // [VI] CANCELLED: link cũ bị thay khi khách bấm "thanh toán lại", hoặc bị đóng vì booking kết thúc -> chỉ log.
            case PaymentEventCommand.PAYMENT_CANCELLED ->
                    // An earlier link replaced by "pay again", or closed because the booking ended.
                    log.info("Payment {} of booking {} cancelled: {}", event.getPaymentId(), booking.getId(), event.getReason());
            // [VI] LINK_CREATED: booking KHÔNG lưu checkoutUrl. Client tự hỏi payment-service
            // (GET /api/v1/payments/by-booking/{bookingId}) để lấy link -> ở đây chỉ log.
            case PaymentEventCommand.PAYMENT_LINK_CREATED ->
                    log.info("Payment link ready for booking {} (payment {})", booking.getId(), event.getPaymentId());
            // [VI] FAILED: một lần trả hỏng không huỷ booking; booking vẫn PENDING chờ khách trả lại hoặc tự hết hạn.
            case PaymentEventCommand.PAYMENT_FAILED ->
                    // One failed attempt does not cancel the booking; it still expires with its seat hold.
                    log.warn("Payment {} failed for booking {}: {}", event.getPaymentId(), booking.getId(), event.getReason());
            default -> log.warn("Unknown payment event type {} for booking {}", type, booking.getId());
        }
    }

    // [VI] Xử lý PAYMENT_SUCCEEDED. Thứ tự kiểm tra:
    //  (a) sai số tiền / currency          -> giữ nguyên booking, log MANUAL CHECK
    //  (b) booking đã CONFIRMED            -> trùng event thì bỏ qua; payment KHÁC thì MANUAL CHECK (trả 2 lần)
    //  (c) booking đang CONFIRMING         -> thử xác nhận ghế lại (lần trước Showtime lỗi tạm)
    //  (d) booking CANCELLED / EXPIRED     -> ghế đã nhả trước đó, chỉ MANUAL CHECK (chưa có refund tự động)
    //  (e) booking PENDING nhưng trả trễ   -> expire + nhả ghế + MANUAL CHECK
    //  (f) booking PENDING, trả đúng hạn   -> CONFIRMING rồi gọi Showtime để chuyển ghế sang BOOKED -> CONFIRMED
    private void onPaymentSucceeded(Booking booking, PaymentEventCommand event) {
        // [VI] (a) So amount + currency trong event với booking trước khi xác nhận ghế.
        if (event.getAmount() == null || event.getAmount().compareTo(booking.getTotalAmount()) != 0
                || event.getCurrency() == null || !event.getCurrency().equalsIgnoreCase(booking.getCurrency())) {
            // Booking stays as it is (still PENDING: the buyer can pay again); the payment is checked manually.
            manualCheck(booking, event.getPaymentId(), "Paid " + event.getAmount() + " " + event.getCurrency()
                    + " but booking total is " + booking.getTotalAmount() + " " + booking.getCurrency());
            return;
        }

        BookingStatus status = booking.getStatus();
        // [VI] (b) Đã xác nhận rồi: so paymentId để phân biệt event giao lại với payment thứ hai.
        if (status == BookingStatus.CONFIRMED) {
            if (event.getPaymentId() != null && !event.getPaymentId().equals(booking.getPaidPaymentId())) {
                manualCheck(booking, event.getPaymentId(), "Booking already paid by " + booking.getPaidPaymentId());
            }
            return;
        }
        // [VI] (c) Đã nhận tiền nhưng chưa chốt được ghế ở Showtime: thử chốt lại.
        if (status == BookingStatus.CONFIRMING) {
            if (event.getPaymentId() != null && !event.getPaymentId().equals(booking.getPaidPaymentId())) {
                manualCheck(booking, event.getPaymentId(), "Booking already paid by " + booking.getPaidPaymentId());
            } else if (seatConfirmation.tryConfirm(booking).changed()) {
                saveBookingPort.save(booking);
            }
            return;
        }
        // [VI] (d) Tiền tới khi booking đã đóng: không đặt lại ghế (có thể đã thuộc người khác), chỉ log để hoàn tay.
        if (status != BookingStatus.PENDING) {
            // CANCELLED / EXPIRED: seats already released.
            manualCheck(booking, event.getPaymentId(), "Payment succeeded but booking is " + status);
            return;
        }

        // Judge by when the buyer paid, not by when the event arrives (webhook / reconcile can be late).
        Instant paidAt = event.getPaidAt() != null ? event.getPaidAt() : Instant.now();
        // [VI] (e) paidAt > expiresAt: hold ghế có thể đã hết -> xử lý như booking không thanh toán.
        if (!booking.wasPaidInTime(paidAt)) {
            // Too late: the seat hold is gone. The booking expires like an unpaid one.
            booking.expire();
            manualCheck(booking, event.getPaymentId(),
                    "Paid at " + paidAt + " after the payment deadline " + booking.getExpiresAt());
            saveBookingPort.save(booking);
            releaseHoldQuietly(booking);
            return;
        }

        // [VI] (f) Luồng thành công: ghi paidPaymentId + paidAt, chuyển CONFIRMING rồi xác nhận ghế ở Showtime.
        // Showtime lỗi tạm -> giữ CONFIRMING cho job bảo trì thử lại; Showtime từ chối hẳn -> huỷ booking.
        booking.markConfirming(event.getPaymentId(), paidAt);
        // Temporary Showtime failure keeps the booking CONFIRMING (saved below) for the maintenance job;
        // a permanent refusal cancels it.
        seatConfirmation.tryConfirm(booking);
        saveBookingPort.save(booking);
    }

    // [VI] PAYMENT_CREATE_FAILED: payment-service không tạo được link PayOS.
    //  - reasonCode = PAYMENT_PROVIDER_ERROR (PayOS lỗi tạm) -> giữ PENDING, khách bấm "thanh toán lại" được.
    //  - lý do khác (INVALID_PAYMENT_REQUEST, request bị từ chối) -> CANCELLED và nhả ghế.
    private void onPaymentCreateFailed(Booking booking, PaymentEventCommand event) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        if (PaymentEventCommand.REASON_PROVIDER_ERROR.equals(event.getReasonCode())) {
            // PayOS trouble is temporary: keep the seats, the buyer can "pay again" until the booking expires.
            log.warn("Payment link for booking {} could not be created ({}); booking stays PENDING",
                    booking.getId(), event.getReason());
            return;
        }
        booking.cancel();
        saveBookingPort.save(booking);
        releaseHoldQuietly(booking);
        log.warn("Booking {} CANCELLED: payment request rejected ({})", booking.getId(), event.getReason());
    }

    // [VI] PAYMENT_EXPIRED: job hết hạn bên payment đã đóng link PayOS -> booking PENDING chuyển EXPIRED, nhả ghế.
    // Chỉ chạy khi PENDING, nên event giao lại hoặc booking đã đóng sẽ không bị đổi.
    private void onPaymentExpired(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        booking.expire();
        saveBookingPort.save(booking);
        releaseHoldQuietly(booking);
        log.info("Booking {} EXPIRED with its payment link", booking.getId());
    }

    /** Money that the booking cannot use: logged for manual check, the booking is not changed. */
    // [VI] Tiền mà booking không dùng được: chỉ ghi log ERROR "MANUAL CHECK" để người vận hành xử lý/hoàn tiền tay.
    private void manualCheck(Booking booking, java.util.UUID paymentId, String reason) {
        log.error("MANUAL CHECK: booking {} payment {}: {}", booking.getId(), paymentId, reason);
    }

    /** After the commit, so a rolled-back state change never gives the seats away. */
    // [VI] Đăng ký nhả ghế SAU KHI commit: nếu transaction rollback thì ghế không bị nhả nhầm.
    private void releaseHoldQuietly(Booking booking) {
        AfterCommit.run(() -> releaseHoldNow(booking));
    }

    // [VI] Gọi Showtime nhả hold. Lỗi thì chỉ log, vì hold ở Showtime tự hết hạn sau 10 phút.
    private void releaseHoldNow(Booking booking) {
        if (booking.getHoldId() == null) {
            return;
        }
        try {
            showtimeClientPort.releaseHold(booking.getHoldId());
        } catch (RuntimeException e) {
            // Showtime expires the hold on its own; the booking state change must not be lost.
            log.warn("Could not release hold {} of booking {}: {}", booking.getHoldId(), booking.getId(), e.getMessage());
        }
    }
}
