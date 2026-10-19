package com.mv.cinemaservice.api.dto.request;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/v1/cinemas and PUT /api/v1/cinemas/{id} (PUT replaces every field). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @Size(max = 100, message = "brand must be at most 100 characters")
    private String brand;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "city is required")
    @Size(max = 100, message = "city must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "district must be at most 100 characters")
    private String district;

    @Size(max = 30, message = "phone must be at most 30 characters")
    private String phone;

    /** Create only: ACTIVE (default) or INACTIVE. Ignored on update; use PATCH /status. */
    private CinemaStatus status;
}
