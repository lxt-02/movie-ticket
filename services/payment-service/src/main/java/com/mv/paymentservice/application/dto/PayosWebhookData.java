package com.mv.paymentservice.application.dto;

import java.time.Instant;

/**
 * The "data" object of a PayOS webhook (only the fields this service uses).
 *
 * @param transactionTime transactionDateTime parsed as Vietnam time (null if missing or not parsable)
 */
public record PayosWebhookData(
        Long orderCode,
        Long amount,
        String currency,
        String paymentLinkId,
        String reference,
        String code,
        String desc,
        Instant transactionTime
) {
}
