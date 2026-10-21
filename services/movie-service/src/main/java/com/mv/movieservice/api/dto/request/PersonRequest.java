package com.mv.movieservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/v1/movies/persons (a director or an actor). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonRequest {

    @NotBlank(message = "Tên là bắt buộc")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String name;

    private String avatarUrl;

    private String biography;
}
