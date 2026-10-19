package com.mv.paymentservice.application.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Current state of a PayOS payment link (GET /v2/payment-requests/{orderCode}).
 *
 * @param status  PENDING, PROCESSING, PAID, UNDERPAID, CANCELLED, EXPIRED, FAILED...
 * @param rawJson original "data" JSON, stored for audit
 */
public record PaymentLinkInfo(
        String status,
        long amount,
        long amountPaid,
        String cancellationReason,
        List<PaymentLinkTransaction> transactions,
        String rawJson
) {
    public PaymentLinkInfo {
        status = status == null ? "" : status.toUpperCase(Locale.ROOT);
        transactions = transactions == null ? List.of() : List.copyOf(transactions);
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    /** CANCELLED / EXPIRED / FAILED: nobody can pay this link any more. */
    public boolean isClosed() {
        return "CANCELLED".equals(status) || "EXPIRED".equals(status) || "FAILED".equals(status);
    }

    /** Latest transfer (by time), or null if none. */
    public PaymentLinkTransaction latestTransaction() {
        return transactions.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(PaymentLinkTransaction::transactionTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }
}
