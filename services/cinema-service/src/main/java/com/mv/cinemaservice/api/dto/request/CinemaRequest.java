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

    @NotBlank(message = "Tên là bắt buộc")
    @Size(max = 255, message = "Tên tối đa 255 ký tự")
    private String name;

    @Size(max = 100, message = "Thương hiệu tối đa 100 ký tự")
    private String brand;

    @NotBlank(message = "Địa chỉ là bắt buộc")
    private String address;

    @NotBlank(message = "Thành phố là bắt buộc")
    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String city;

    @Size(max = 100, message = "Quận / huyện tối đa 100 ký tự")
    private String district;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    /** Create only: ACTIVE (default) or INACTIVE. Ignored on update; use PATCH /status. */
    private CinemaStatus status;
}
