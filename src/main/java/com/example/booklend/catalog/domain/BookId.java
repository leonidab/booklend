package com.example.booklend.catalog.domain;

import java.util.UUID;

public record BookId(UUID value) {

    public static BookId newId() {
        return new BookId(UUID.randomUUID());
    }

    public static BookId of(String value) {
        return new BookId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
