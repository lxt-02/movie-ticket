package com.mv.showtimeservice.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
