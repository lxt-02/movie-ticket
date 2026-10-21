package com.mv.userservice.api.dto.request;

import com.mv.userservice.domain.model.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {
    @NotNull(message = "Trạng thái là bắt buộc")
    private UserStatus status;
}
