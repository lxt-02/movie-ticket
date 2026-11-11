package com.mv.paymentservice.infrastructure.payos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * payos.* settings. Keys come from environment variables (PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY);
 * never hardcode them.
 */
@ConfigurationProperties(prefix = "payos")
public record PayosProperties(
        String clientId,
        String apiKey,
        String checksumKey,
        String baseUrl,
        String returnUrl,
        String cancelUrl,
        Long paymentExpirationMinutes,
        Integer connectTimeoutMs,
        Integer readTimeoutMs,
        /**
         * Demo/test: số tiền gửi sang PayOS = số tiền thật / amountDivisor (mặc định 1 = trả đúng giá).
         * Chỉ áp dụng ở biên PayOS (adapter); payment, booking và event vẫn giữ số tiền thật.
         */
        Long amountDivisor
) {
    public PayosProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api-merchant.payos.vn";
        }
        if (paymentExpirationMinutes == null || paymentExpirationMinutes <= 0) {
            paymentExpirationMinutes = 15L;
        }
        if (connectTimeoutMs == null || connectTimeoutMs <= 0) {
            connectTimeoutMs = 5000;
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            readTimeoutMs = 15000;
        }
        if (amountDivisor == null || amountDivisor <= 0) {
            amountDivisor = 1L;
        }
    }
}
