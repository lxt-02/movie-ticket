package com.mv.userservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PUT /api/v1/users/me: name and avatar (email is part of login, not editable here). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên là bắt buộc")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @Size(max = 1000, message = "Đường dẫn ảnh đại diện tối đa 1000 ký tự")
    private String avatarUrl;
}
