package com.mv.paymentservice.domain.model.payment.enums;

// [VI] Vòng đời payment: PENDING -> SUCCESS | FAILED | EXPIRED | CANCELLED (4 trạng thái kết thúc).
// PROCESSING hiện CHƯA có code nào đặt, chỉ được coi là "chưa kết thúc" trong isTerminal() (dành sẵn).
// Đừng nhầm với PROCESSING của bảng outbox_messages.
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this != PENDING && this != PROCESSING;
    }
}
