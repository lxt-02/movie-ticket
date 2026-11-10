package com.mv.apigateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * The gateway is the only place that checks the Keycloak access token (Authorization: Bearer ...).
 * Services behind it trust X-User-Id / X-User-Role, which {@link UserHeaderRelayFilter} builds from the token
 * (client-sent copies are removed).
 * <ul>
 *     <li>Public: catalogue GETs (movies, cinemas, screens, showtimes), movie search, PayOS webhook, health, rate-limit demo.</li>
 *     <li>Everything else needs a valid token. Admin-only actions are still checked by each service (X-User-Role).</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, SecurityProperties properties) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // So 401 / 403 written by Security also carry CORS headers (the browser can read them).
                .cors(cors -> cors.configurationSource(corsSource(properties)))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus", "/rate-limit/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/payos/webhook").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/v1/movies", "/api/v1/movies/**",
                                "/api/v1/cinemas", "/api/v1/cinemas/**",
                                "/api/v1/screens/**", "/api/v1/screen-types", "/api/v1/seat-types",
                                "/api/v1/showtimes", "/api/v1/showtimes/**",
                                "/api/v1/search/**",
                                "/internal/movies/**", "/internal/cinemas/**", "/internal/screens/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(JsonSecurityErrors::unauthorized)
                        .accessDeniedHandler(JsonSecurityErrors::forbidden))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(JsonSecurityErrors::unauthorized)
                        .accessDeniedHandler(JsonSecurityErrors::forbidden)
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder(properties))));
        return http.build();
    }

    /** Keys are downloaded on the first token, so the gateway starts even if Keycloak is not up yet. */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuerUri()));
        return decoder;
    }

    private static UrlBasedCorsConfigurationSource corsSource(SecurityProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(properties.getAllowedOrigins().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Authorization", "Idempotency-Key", "X-Api-Key", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("Retry-After", "X-RateLimit-Remaining", "X-RateLimit-Burst-Capacity",
                "X-RateLimit-Replenish-Rate", "X-RateLimit-Requested-Tokens", "X-Served-By", "X-Correlation-Id"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
