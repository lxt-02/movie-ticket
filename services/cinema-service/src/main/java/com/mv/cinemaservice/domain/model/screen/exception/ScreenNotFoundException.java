package com.mv.cinemaservice.domain.model.screen.exception;

import com.mv.cinemaservice.domain.model.cinema.exception.DomainException;

import java.util.UUID;

public class ScreenNotFoundException extends DomainException {
    public ScreenNotFoundException(UUID screenId) {
        super("Screen not found with id: " + screenId);
    }
}
