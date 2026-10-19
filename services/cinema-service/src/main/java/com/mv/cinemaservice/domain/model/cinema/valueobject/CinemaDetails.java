package com.mv.cinemaservice.domain.model.cinema.valueobject;

import com.mv.cinemaservice.domain.model.cinema.exception.InvalidCinemaException;

import java.util.regex.Pattern;

/** Editable information of a cinema, validated on construction. */
public record CinemaDetails(
        String name,
        String brand,
        String address,
        String city,
        String district,
        String phone
) {
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-. ]{6,30}$");

    public CinemaDetails {
        name = required(name, "name", 255);
        address = required(address, "address", 1000);
        city = required(city, "city", 100);
        brand = optional(brand, "brand", 100);
        district = optional(district, "district", 100);
        phone = optional(phone, "phone", 30);
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new InvalidCinemaException("phone has an invalid format");
        }
    }

    private static String required(String value, String field, int max) {
        String v = optional(value, field, max);
        if (v == null) {
            throw new InvalidCinemaException(field + " must not be blank");
        }
        return v;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new InvalidCinemaException(field + " must be at most " + max + " characters");
        }
        return v;
    }
}
