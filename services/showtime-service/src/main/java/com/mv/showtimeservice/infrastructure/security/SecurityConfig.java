package com.mv.showtimeservice.infrastructure.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * The token is checked once, at the gateway. Here the caller comes from the gateway headers
 * ({@link GatewayHeaderAuthenticationFilter} → SecurityContextHolder).
 * <ul>
 *     <li>Public paths of this service and {@code /internal/**} (service-to-service, not routed by the gateway): open.</li>
 *     <li>Everything else: a caller is required (401 otherwise). Admin-only actions check the role in the
 *     controllers ({@code AdminAccess} / {@link CurrentUser}).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new GatewayHeaderAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/showtimes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(JsonSecurityErrors::unauthorized)
                        .accessDeniedHandler(JsonSecurityErrors::forbidden));
        return http.build();
    }
}
