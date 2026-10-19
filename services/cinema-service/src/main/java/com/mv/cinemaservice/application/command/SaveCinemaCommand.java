package com.mv.cinemaservice.application.command;

import com.mv.cinemaservice.domain.model.cinema.enums.CinemaStatus;
import lombok.Builder;
import lombok.Getter;

/** Create / update a cinema. {@code status} is only used on create. */
@Getter
@Builder
public class SaveCinemaCommand {
    private String name;
    private String brand;
    private String address;
    private String city;
    private String district;
    private String phone;
    private CinemaStatus status;
}
