package com.mv.paymentservice.application.dto;

import java.time.Instant;

/**
 * One bank transfer received on a PayOS payment link.
 *
 * @param transactionTime when the bank recorded the transfer (null if PayOS did not send a parsable time)
 */
public record PaymentLinkTransaction(String reference, long amount, Instant transactionTime) {
}
