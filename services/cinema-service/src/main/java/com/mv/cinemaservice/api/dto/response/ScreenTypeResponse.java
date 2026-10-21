package com.mv.cinemaservice.api.dto.response;

import java.util.UUID;

public record ScreenTypeResponse(UUID id, String name, String description) {
}
