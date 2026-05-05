package com.example.booklend.lending.domain;

import java.util.UUID;

public record LoanId(UUID value) {

    public static LoanId newId() {
        return new LoanId(UUID.randomUUID());
    }

    public static LoanId of(String value) {
        return new LoanId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
