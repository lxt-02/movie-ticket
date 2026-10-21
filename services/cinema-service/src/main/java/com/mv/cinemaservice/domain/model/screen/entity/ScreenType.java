package com.mv.cinemaservice.domain.model.screen.entity;

import java.util.UUID;

/** Projection type of a screen (2D, IMAX...). Reference data. */
public record ScreenType(UUID id, String name, String description) {
}
