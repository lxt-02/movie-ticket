package com.mv.apigateway.ratelimit;

import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyFactory {

    public String userKey(String userId, String scope) {
        return key("user", userId, scope);
    }

    public String ipKey(String clientIp, String scope) {
        return key("ip", clientIp, scope);
    }

    public String apiKey(String keyFingerprint, String scope) {
        return key("apikey", keyFingerprint, scope);
    }

    private String key(String identityType, String identity, String scope) {
        return "rl:%s:%s:%s".formatted(identityType, normalize(identity), normalize(scope));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
