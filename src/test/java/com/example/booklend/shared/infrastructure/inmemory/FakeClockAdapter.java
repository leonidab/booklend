package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.shared.application.port.out.ClockPort;

import java.time.Instant;

public class FakeClockAdapter implements ClockPort {

    private Instant current;

    public FakeClockAdapter(Instant initial) {
        this.current = initial;
    }

    public void advanceTo(Instant instant) {
        this.current = instant;
    }

    @Override
    public Instant now() {
        return current;
    }
}
