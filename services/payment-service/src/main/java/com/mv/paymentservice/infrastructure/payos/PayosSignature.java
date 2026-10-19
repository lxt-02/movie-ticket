package com.mv.paymentservice.infrastructure.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PayOS signatures: HMAC-SHA256 (hex) with the checksum key.
 * <ul>
 *     <li>Create payment link: over "amount=..&cancelUrl=..&description=..&orderCode=..&returnUrl=..".</li>
 *     <li>Webhook: over every field of "data", sorted by key, "key=value" joined by "&";
 *     null / "null" / "undefined" become empty strings, arrays are JSON with sorted keys.</li>
 * </ul>
 */
public final class PayosSignature {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private PayosSignature() {
    }

    public static String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    public static String paymentRequestData(long amount, String cancelUrl, String description,
                                            long orderCode, String returnUrl) {
        return "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
    }

    public static String webhookData(JsonNode data, ObjectMapper mapper) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            sorted.put(field.getKey(), field.getValue());
        }
        List<String> parts = new ArrayList<>(sorted.size());
        for (Map.Entry<String, JsonNode> entry : sorted.entrySet()) {
            parts.add(entry.getKey() + "=" + valueToString(entry.getValue(), mapper));
        }
        return String.join("&", parts);
    }

    /** Constant-time comparison of two hex signatures (case-insensitive). */
    public static boolean matches(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHex.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actualHex.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String valueToString(JsonNode value, ObjectMapper mapper) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "";
        }
        if (value.isTextual()) {
            String text = value.textValue();
            return "null".equals(text) || "undefined".equals(text) ? "" : text;
        }
        if (value.isNumber()) {
            // Same text as JavaScript's String(number): 2000, not 2000.0
            return value.decimalValue().stripTrailingZeros().toPlainString();
        }
        if (value.isArray()) {
            ArrayNode sortedArray = mapper.createArrayNode();
            for (JsonNode item : value) {
                sortedArray.add(item.isObject() ? sortObject((ObjectNode) item, mapper) : item);
            }
            return sortedArray.toString();
        }
        if (value.isObject()) {
            return sortObject((ObjectNode) value, mapper).toString();
        }
        return value.asText();
    }

    private static ObjectNode sortObject(ObjectNode node, ObjectMapper mapper) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        node.fields().forEachRemaining(e -> sorted.put(e.getKey(), e.getValue()));
        ObjectNode result = mapper.createObjectNode();
        sorted.forEach(result::set);
        return result;
    }
}
