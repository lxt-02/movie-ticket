package com.mv.paymentservice.infrastructure.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.mv.paymentservice.application.dto.PaymentLink;
import com.mv.paymentservice.application.dto.PaymentLinkInfo;
import com.mv.paymentservice.application.dto.PaymentLinkTransaction;
import com.mv.paymentservice.application.dto.PaymentLinkRequest;
import com.mv.paymentservice.application.dto.PayosWebhook;
import com.mv.paymentservice.application.dto.PayosWebhookData;
import com.mv.paymentservice.application.exception.PaymentGatewayException;
import com.mv.paymentservice.application.port.out.PaymentGatewayPort;
import com.mv.paymentservice.domain.model.payment.exception.InvalidWebhookException;
import com.mv.paymentservice.infrastructure.config.JsonCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PayOS merchant API (https://payos.vn/docs) called over HTTPS with x-client-id / x-api-key headers.
 * Request and webhook signatures use HMAC-SHA256 with the checksum key (see {@link PayosSignature}).
 */
@Slf4j
@Component
public class PayosPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String SUCCESS_CODE = "00";

    private final PayosProperties properties;
    private final JsonCodec json;
    private final RestClient restClient;

    public PayosPaymentGatewayAdapter(PayosProperties properties, JsonCodec json) {
        this.properties = properties;
        this.json = json;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-client-id", nullToEmpty(properties.clientId()))
                .defaultHeader("x-api-key", nullToEmpty(properties.apiKey()))
                .build();
    }

    @Override
    public PaymentLink createPaymentLink(PaymentLinkRequest request) {
        requireCredentials();
        String returnUrl = properties.returnUrl();
        String cancelUrl = properties.cancelUrl();
        if (isBlank(returnUrl) || isBlank(cancelUrl)) {
            throw new PaymentGatewayException("payos.return-url and payos.cancel-url must be configured");
        }

        String signature = PayosSignature.hmacSha256Hex(properties.checksumKey(),
                PayosSignature.paymentRequestData(request.amount(), cancelUrl, request.description(),
                        request.orderCode(), returnUrl));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", request.orderCode());
        body.put("amount", request.amount());
        body.put("description", request.description());
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("expiredAt", request.expiredAt().getEpochSecond());
        body.put("signature", signature);

        JsonNode data = call("create payment link " + request.orderCode(), () -> restClient.post()
                .uri("/v2/payment-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.write(body))
                .retrieve()
                .body(String.class));

        String paymentLinkId = text(data, "paymentLinkId");
        String checkoutUrl = text(data, "checkoutUrl");
        if (isBlank(paymentLinkId) || isBlank(checkoutUrl)) {
            throw new PaymentGatewayException("PayOS response has no paymentLinkId/checkoutUrl for orderCode " + request.orderCode());
        }
        return new PaymentLink(paymentLinkId, checkoutUrl, text(data, "qrCode"));
    }

    @Override
    public void cancelPaymentLink(long orderCode, String reason) {
        requireCredentials();
        Map<String, Object> body = Map.of("cancellationReason", reason);
        call("cancel payment link " + orderCode, () -> restClient.post()
                .uri("/v2/payment-requests/{id}/cancel", orderCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.write(body))
                .retrieve()
                .body(String.class));
    }

    @Override
    public PaymentLinkInfo getPaymentLinkInfo(long orderCode) {
        requireCredentials();
        JsonNode data = call("get payment link " + orderCode, () -> restClient.get()
                .uri("/v2/payment-requests/{id}", orderCode)
                .retrieve()
                .body(String.class));

        List<PaymentLinkTransaction> transactions = new ArrayList<>();
        JsonNode list = data.get("transactions");
        if (list != null && list.isArray()) {
            for (JsonNode tx : list) {
                Long amount = longValue(tx, "amount");
                transactions.add(new PaymentLinkTransaction(
                        text(tx, "reference"),
                        amount == null ? 0L : amount,
                        PayosTime.parse(text(tx, "transactionDateTime"))));
            }
        }
        Long amount = longValue(data, "amount");
        Long amountPaid = longValue(data, "amountPaid");
        return new PaymentLinkInfo(
                text(data, "status"),
                amount == null ? 0L : amount,
                amountPaid == null ? 0L : amountPaid,
                text(data, "cancellationReason"),
                transactions,
                data.toString());
    }

    @Override
    public PayosWebhook verifyWebhook(String rawBody) {
        if (isBlank(properties.checksumKey())) {
            throw new InvalidWebhookException("payos.checksum-key is not configured");
        }
        if (isBlank(rawBody)) {
            throw new InvalidWebhookException("Empty webhook body");
        }

        JsonNode root;
        try {
            root = json.readTree(rawBody);
        } catch (IllegalArgumentException e) {
            throw new InvalidWebhookException("Malformed webhook body");
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isObject()) {
            throw new InvalidWebhookException("Webhook has no data object");
        }

        String expected = PayosSignature.hmacSha256Hex(properties.checksumKey(),
                PayosSignature.webhookData(data, json.mapper()));
        if (!PayosSignature.matches(expected, text(root, "signature"))) {
            throw new InvalidWebhookException("Invalid webhook signature");
        }

        PayosWebhookData webhookData = new PayosWebhookData(
                longValue(data, "orderCode"),
                longValue(data, "amount"),
                text(data, "currency"),
                text(data, "paymentLinkId"),
                text(data, "reference"),
                text(data, "code"),
                text(data, "desc"),
                PayosTime.parse(text(data, "transactionDateTime"))
        );
        return new PayosWebhook(
                text(root, "code"),
                text(root, "desc"),
                root.path("success").asBoolean(false),
                webhookData,
                rawBody
        );
    }

    /** Runs the HTTP call and returns "data" when the PayOS response code is "00". */
    private JsonNode call(String action, HttpCall httpCall) {
        String responseBody;
        try {
            responseBody = httpCall.execute();
        } catch (RestClientResponseException e) {
            throw new PaymentGatewayException("PayOS " + action + " failed: HTTP " + e.getStatusCode().value()
                    + " " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new PaymentGatewayException("PayOS " + action + " failed: " + e.getMessage(), e);
        }
        if (isBlank(responseBody)) {
            throw new PaymentGatewayException("PayOS " + action + " returned an empty body");
        }

        JsonNode root;
        try {
            root = json.readTree(responseBody);
        } catch (IllegalArgumentException e) {
            throw new PaymentGatewayException("PayOS " + action + " returned malformed JSON", e);
        }
        String code = text(root, "code");
        if (!SUCCESS_CODE.equals(code)) {
            throw new PaymentGatewayException("PayOS " + action + " failed: code=" + code + ", desc=" + text(root, "desc"));
        }
        JsonNode data = root.get("data");
        return data == null ? json.mapper().createObjectNode() : data;
    }

    private void requireCredentials() {
        if (isBlank(properties.clientId()) || isBlank(properties.apiKey()) || isBlank(properties.checksumKey())) {
            throw new PaymentGatewayException(
                    "PayOS credentials missing: set PAYOS_CLIENT_ID, PAYOS_API_KEY and PAYOS_CHECKSUM_KEY");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        try {
            return Long.parseLong(value.asText().trim());
        } catch (NumberFormatException e) {
            throw new InvalidWebhookException("Webhook field " + field + " is not a number");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface HttpCall {
        String execute();
    }
}
