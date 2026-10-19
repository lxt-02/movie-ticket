package com.mv.paymentservice.infrastructure.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expected values were computed independently with Python's hmac module.
 */
class PayosSignatureTest {

    private static final String KEY = "test-checksum-key";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void webhookDataIsSortedAndNullsBecomeEmpty() throws Exception {
        JsonNode data = mapper.readTree("""
                {"orderCode":123,"amount":3000,"description":"VQRIO123","accountNumber":"12345678",
                 "reference":"TF230204212323","transactionDateTime":"2023-02-04 18:25:00","currency":"VND",
                 "paymentLinkId":"124c33293c43417ab7879e14c8d9eb18","code":"00","desc":"Thành công",
                 "counterAccountBankId":"","counterAccountBankName":"","counterAccountName":null,
                 "counterAccountNumber":"","virtualAccountName":"","virtualAccountNumber":""}
                """);

        String canonical = PayosSignature.webhookData(data, mapper);

        assertThat(canonical).isEqualTo("accountNumber=12345678&amount=3000&code=00&counterAccountBankId="
                + "&counterAccountBankName=&counterAccountName=&counterAccountNumber=&currency=VND"
                + "&desc=Thành công&description=VQRIO123&orderCode=123"
                + "&paymentLinkId=124c33293c43417ab7879e14c8d9eb18&reference=TF230204212323"
                + "&transactionDateTime=2023-02-04 18:25:00&virtualAccountName=&virtualAccountNumber=");
        assertThat(PayosSignature.hmacSha256Hex(KEY, canonical))
                .isEqualTo("15cd38e52473536ad13caec70a9d5fd6446d63812d84e3ab2af193feb5dded64");
    }

    @Test
    void paymentRequestSignature() {
        String data = PayosSignature.paymentRequestData(150000, "http://localhost:3000/payment/cancel",
                "SP1234567", 1234561234567L, "http://localhost:3000/payment/success");

        assertThat(PayosSignature.hmacSha256Hex(KEY, data))
                .isEqualTo("1dd27a8fe86ec7c9fb209881a5290b02b100d61a2e9bb3ad0a3c2e22a8e22556");
    }

    @Test
    void matchesIsCaseInsensitiveAndRejectsNull() {
        assertThat(PayosSignature.matches("abcdef", "ABCDEF")).isTrue();
        assertThat(PayosSignature.matches("abcdef", "abcdee")).isFalse();
        assertThat(PayosSignature.matches("abcdef", null)).isFalse();
    }
}
