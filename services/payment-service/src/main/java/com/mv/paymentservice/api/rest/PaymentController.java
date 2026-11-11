package com.mv.paymentservice.api.rest;

import com.mv.paymentservice.api.dto.response.ApiResponse;
import com.mv.paymentservice.api.dto.response.PaymentResponse;
import com.mv.paymentservice.api.mapper.PaymentDtoMapper;
import com.mv.paymentservice.application.port.in.GetPaymentUseCase;
import com.mv.paymentservice.application.port.in.SyncPaymentUseCase;
import com.mv.paymentservice.application.query.GetPaymentQuery;
import com.mv.paymentservice.domain.model.payment.aggregate.Payment;
import com.mv.paymentservice.domain.model.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Frontend API. Payments are created through Kafka (CREATE_PAYMENT from booking-service), so the frontend polls
 * the booking's latest payment to get checkoutUrl / qrCode and its status. When the buyer comes back on returnUrl
 * or cancelUrl, the frontend calls a /sync endpoint: the server asks PayOS directly and never trusts the redirect.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
// [VI] API cho frontend. Booking KHÔNG trả checkoutUrl trong response đặt vé (201): link được tạo bất đồng bộ
// qua Kafka. Client lấy bookingId từ response 201 rồi polling endpoint by-booking ở đây để lấy checkoutUrl/qrCode.
// Lý do tách: tạo link phụ thuộc PayOS (chậm/lỗi) -> đặt vé vẫn trả 201 ngay cả khi PayOS hoặc Kafka đang sập.
public class PaymentController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final SyncPaymentUseCase syncPaymentUseCase;
    private final PaymentDtoMapper paymentDtoMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        Payment payment = getPaymentUseCase.getPaymentById(new GetPaymentQuery(id));
        requireOwner(payment, userId);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    /** Latest payment (PayOS link) of a booking. 404 until booking-service's CREATE_PAYMENT is processed. */
    // [VI] Polling sau khi đặt vé: trả 404 cho tới khi CREATE_PAYMENT được xử lý (relay booking 2 s + consumer +
    // gọi PayOS), sau đó trả payment có checkoutUrl, qrCode.
    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getLatestPaymentByBooking(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        Payment payment = getPaymentUseCase.getLatestPaymentByBookingId(bookingId);
        requireOwner(payment, userId);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    /** Checks the payment with PayOS now and returns its updated state. */
    @PostMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncPayment(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        requireOwner(getPaymentUseCase.getPaymentById(new GetPaymentQuery(id)), userId);
        Payment payment = syncPaymentUseCase.syncPayment(id);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    /** Same as /{id}/sync for the booking's latest payment. */
    // [VI] Khách quay lại từ trang PayOS: ép hỏi PayOS ngay, không chờ webhook hay job reconcile 60 s.
    @PostMapping("/by-booking/{bookingId}/sync")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncLatestPaymentByBooking(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        requireOwner(getPaymentUseCase.getLatestPaymentByBookingId(bookingId), userId);
        Payment payment = syncPaymentUseCase.syncLatestPaymentOfBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(paymentDtoMapper.toResponse(payment)));
    }

    /** Until JWT is in place the caller identity is the required X-User-Id header set by the gateway. */
    // [VI] So X-User-Id (gateway đặt từ JWT) với chủ payment: không phải chủ thì trả 404 -> không xem được link người khác.
    private void requireOwner(Payment payment, UUID userId) {
        if (userId == null || !userId.equals(payment.getUserId())) {
            throw new PaymentNotFoundException(payment.getId());
        }
    }
}
