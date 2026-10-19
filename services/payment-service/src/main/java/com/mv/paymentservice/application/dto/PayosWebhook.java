package com.mv.paymentservice.application.dto;

/**
 * PayOS webhook whose signature has already been verified with the checksum key.
 *
 * @param rawBody original JSON, stored in payment_transactions.raw_response
 */
public record PayosWebhook(String code, String desc, boolean success, PayosWebhookData data, String rawBody) {

    public boolean isPaid() {
        return success && "00".equals(code) && data != null && "00".equals(data.code());
    }
}
