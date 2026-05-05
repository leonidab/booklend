package com.example.booklend.lending.domain;

import java.util.UUID;

public record ReservationId(UUID value) {

    public static ReservationId newId() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(String value) {
        return new ReservationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
