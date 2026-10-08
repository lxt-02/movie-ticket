package com.mv.apigateway.config;

import com.mv.apigateway.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter defaultRedisRateLimiter(RateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getReplenishRate(),
                properties.getBurstCapacity(),
                properties.getRequestedTokens()
        );
    }
}
