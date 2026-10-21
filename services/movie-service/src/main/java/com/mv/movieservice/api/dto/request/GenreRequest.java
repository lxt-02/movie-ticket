package com.mv.movieservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/v1/movies/genres. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequest {

    @NotBlank(message = "Tên thể loại là bắt buộc")
    @Size(max = 100, message = "Tên thể loại tối đa 100 ký tự")
    private String name;
}
