package com.mv.paymentservice.application.dto;

/**
 * PayOS payment link returned by "create payment request".
 */
public record PaymentLink(String paymentLinkId, String checkoutUrl, String qrCode) {
}
