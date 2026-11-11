package com.mv.bookingservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An event from payment-service (topic payment-events).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// [VI] Dạng nội bộ của một event từ payment-service, do PaymentEventListener.toCommand() dựng từ JSON.
// Các hằng số dưới đây PHẢI khớp contracts/booking-payment/contract.json (eventTypes, createFailedReasons);
// BookingPaymentContractTest sẽ fail nếu lệch.
public class PaymentEventCommand {
    public static final String PAYMENT_LINK_CREATED = "PAYMENT_LINK_CREATED";
    public static final String PAYMENT_CREATE_FAILED = "PAYMENT_CREATE_FAILED";
    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_EXPIRED = "PAYMENT_EXPIRED";
    public static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";

    /** PAYMENT_CREATE_FAILED reason codes sent by payment-service. */
    // [VI] Mã lý do của PAYMENT_CREATE_FAILED: INVALID = huỷ booking, PROVIDER_ERROR = giữ PENDING.
    public static final String REASON_INVALID_REQUEST = "INVALID_PAYMENT_REQUEST";
    public static final String REASON_PROVIDER_ERROR = "PAYMENT_PROVIDER_ERROR";

    // [VI] Những trường booking thực sự dùng: eventId + bookingId (thiếu thì bỏ qua message),
    // amount + currency (so với booking), paidAt (xét đúng hạn), paymentId (ghi vào paidPaymentId).
    private UUID eventId;
    private String eventType;
    private UUID bookingId;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    /** When the bank transfer happened (from PayOS), used to decide if the booking was paid in time. */
    private Instant paidAt;
    /** PAYMENT_CREATE_FAILED code: INVALID_PAYMENT_REQUEST or PAYMENT_PROVIDER_ERROR. */
    private String reasonCode;
    private String reason;
}
