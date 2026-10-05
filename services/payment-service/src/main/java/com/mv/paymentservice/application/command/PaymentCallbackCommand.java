package com.mv.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackCommand {
    private UUID paymentId;
    private String providerTransactionId;
    private boolean success;
    private String failureReason;
}
