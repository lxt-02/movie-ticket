package com.mv.apigateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit settings bound from {@code app.rate-limit.*}.
 * Each identity type (user, IP, API key) has its own Token Bucket policy.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /**
     * DEV/DEMO ONLY: trust the client-supplied X-User-Id header as the caller identity.
     * A client can rotate this header to get a new bucket, so it must be false in production,
     * where the user ID comes from a validated JWT principal instead.
     */
    private boolean trustUserIdHeader = false;

    /**
     * Number of trusted proxies / load balancers in front of the Gateway.
     * 0 = Gateway is exposed directly: use the TCP remote address and ignore X-Forwarded-For.
     * n > 0 = take the client IP from X-Forwarded-For, trusting only the last n entries
     * (the ones appended by our own proxies), so clients cannot spoof it.
     */
    private int trustedProxyHops = 0;

    private Policy user = new Policy(1, 3, 1);
    private Policy ip = new Policy(1, 5, 1);
    private Policy apiKey = new Policy(2, 5, 1);

    public boolean isTrustUserIdHeader() {
        return trustUserIdHeader;
    }

    public void setTrustUserIdHeader(boolean trustUserIdHeader) {
        this.trustUserIdHeader = trustUserIdHeader;
    }

    public int getTrustedProxyHops() {
        return trustedProxyHops;
    }

    public void setTrustedProxyHops(int trustedProxyHops) {
        this.trustedProxyHops = trustedProxyHops;
    }

    public Policy getUser() {
        return user;
    }

    public void setUser(Policy user) {
        this.user = user;
    }

    public Policy getIp() {
        return ip;
    }

    public void setIp(Policy ip) {
        this.ip = ip;
    }

    public Policy getApiKey() {
        return apiKey;
    }

    public void setApiKey(Policy apiKey) {
        this.apiKey = apiKey;
    }

    /** Token Bucket parameters used by {@code RedisRateLimiter}. */
    public static class Policy {

        /** Tokens added to the bucket per second (sustained rate). */
        private int replenishRate;
        /** Maximum tokens the bucket can hold (burst allowance). */
        private int burstCapacity;
        /** Tokens consumed by one request. */
        private int requestedTokens;

        public Policy() {
        }

        public Policy(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
            this.requestedTokens = requestedTokens;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }
}
