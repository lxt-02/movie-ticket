package com.mv.showtimeservice.infrastructure.adapter;

import com.mv.showtimeservice.application.exception.DependencyUnavailableException;
import com.mv.showtimeservice.infrastructure.client.FeignErrors;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/** Turns Feign errors of movie / cinema calls into errors the admin understands. */
@Slf4j
final class RemoteCalls {

    private RemoteCalls() {
    }

    /**
     * 404 → 400 with {@code notFound} (the id given by the admin does not exist); 400 / 409 → 400 with the other
     * service's message; down / 5xx → 503.
     */
    static RuntimeException translate(FeignException e, String serviceLabel, String notFound) {
        if (e.status() == 404) {
            return new IllegalArgumentException(notFound);
        }
        if (FeignErrors.isUnavailable(e)) {
            return unavailable(serviceLabel, e);
        }
        return new IllegalArgumentException(FeignErrors.messageOr(e,
                "Dịch vụ " + serviceLabel + " từ chối yêu cầu (HTTP " + e.status() + ")"));
    }

    static DependencyUnavailableException unavailable(String serviceLabel, RuntimeException e) {
        log.warn("Call to {} service failed: {}", serviceLabel,
                e instanceof FeignException fe ? FeignErrors.target(fe) + " HTTP " + fe.status() : e.toString());
        return new DependencyUnavailableException(
                "Dịch vụ " + serviceLabel + " tạm thời không khả dụng, vui lòng thử lại sau", e);
    }
}
