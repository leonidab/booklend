package com.example.booklend.catalog.domain;

import java.util.Objects;

public record ISBN(String value) {

    public ISBN {
        Objects.requireNonNull(value, "ISBN must not be null");
        value = value.replaceAll("[\\s-]", "");
        if (!value.matches("\\d{10}|\\d{13}")) {
            throw new IllegalArgumentException("Invalid ISBN (must be 10 or 13 digits): " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
