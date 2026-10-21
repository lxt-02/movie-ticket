package com.mv.movieservice.api.dto.response;

import java.util.UUID;

public record PersonResponse(UUID id, String name, String avatarUrl, String biography) {
}
