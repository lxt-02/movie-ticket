package com.mv.paymentservice.application.service;

/**
 * Result of checking / closing a payment against PayOS.
 */
// [VI] Kết quả đối chiếu payment với PayOS: PAID (đã có tiền), CLOSED (link đã đóng),
// STILL_OPEN (chưa quyết được: PayOS đang xử lý, lỗi gọi PayOS, hoặc lệch số tiền).
public enum GatewaySyncOutcome {
    /** PayOS reported the money: payment is now SUCCESS (or late paid). */
    PAID,
    /** Link is closed: payment is now CANCELLED / EXPIRED / FAILED. */
    CLOSED,
    /** Nothing decided yet (still PENDING/PROCESSING/UNDERPAID on PayOS, or amount mismatch). */
    STILL_OPEN
}
