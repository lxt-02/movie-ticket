package com.mv.paymentservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GetPaymentQuery {
    private final UUID paymentId;
}
