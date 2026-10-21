package com.mv.cinemaservice.domain.model.cinema.exception;

import java.util.UUID;

public class CinemaNotFoundException extends DomainException {
    public CinemaNotFoundException(UUID cinemaId) {
        super("Không tìm thấy rạp có id: " + cinemaId);
    }
}
