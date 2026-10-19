package com.mv.paymentservice.domain.model.payment.entity;

import com.mv.paymentservice.domain.model.payment.enums.TransactionStatus;
import com.mv.paymentservice.domain.model.payment.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One gateway callback stored for audit. Each PayOS webhook is stored exactly once,
 * keyed by (paymentId, transactionCode, gatewayTransactionId).
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentTransaction {
    private UUID id;
    private UUID paymentId;
    /** PayOS data.reference */
    private String transactionCode;
    /** PayOS data.paymentLinkId */
    private String gatewayTransactionId;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private BigDecimal amount;
    /** Original webhook JSON */
    private String rawResponse;
    private Instant createdAt;
}
