package com.example.booklend.member.domain;

import java.util.UUID;

public record MemberId(UUID value) {

    public static MemberId newId() {
        return new MemberId(UUID.randomUUID());
    }

    public static MemberId of(String value) {
        return new MemberId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
