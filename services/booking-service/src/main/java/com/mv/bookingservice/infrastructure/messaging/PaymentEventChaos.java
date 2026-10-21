package com.mv.bookingservice.infrastructure.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DEMO ONLY fault injection: while "failing" is on, every payment event on the main topic throws as if the booking
 * database were down, so the retry → dead-letter topic → replay path can be shown live.
 * Disabled unless {@code booking.demo.chaos-enabled=true}; never enable it in production.
 */
@Component
public class PaymentEventChaos {

    private final boolean allowed;
    private final AtomicBoolean failing = new AtomicBoolean(false);

    public PaymentEventChaos(@Value("${booking.demo.chaos-enabled:false}") boolean allowed) {
        this.allowed = allowed;
    }

    public boolean allowed() {
        return allowed;
    }

    public boolean isFailing() {
        return allowed && failing.get();
    }

    /** @throws IllegalStateException if chaos is not enabled for this instance */
    public void setFailing(boolean on) {
        if (!allowed) {
            throw new IllegalStateException("Chaos is disabled (set booking.demo.chaos-enabled=true for the demo)");
        }
        failing.set(on);
    }

    /** Called before a payment event is applied. */
    public void maybeFail() {
        if (isFailing()) {
            throw new IllegalStateException("CHAOS: simulated booking database outage while applying a payment event");
        }
    }
}
