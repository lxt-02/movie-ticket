package com.mv.apigateway.config;

import com.mv.apigateway.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * One RedisRateLimiter (Token Bucket on Redis) per policy.
 * Routes pick a limiter by bean name, e.g. {@code "#{@userRateLimiter}"}.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    // @Primary: Spring Cloud Gateway injects one default RateLimiter by type
    // when it creates the RequestRateLimiter filter factory.
    @Bean
    @Primary
    public RedisRateLimiter userRateLimiter(RateLimitProperties properties) {
        return create(properties.getUser());
    }

    @Bean
    public RedisRateLimiter ipRateLimiter(RateLimitProperties properties) {
        return create(properties.getIp());
    }

    @Bean
    public RedisRateLimiter apiKeyRateLimiter(RateLimitProperties properties) {
        return create(properties.getApiKey());
    }

    private RedisRateLimiter create(RateLimitProperties.Policy policy) {
        return new RedisRateLimiter(
                policy.getReplenishRate(),
                policy.getBurstCapacity(),
                policy.getRequestedTokens()
        );
    }
}
