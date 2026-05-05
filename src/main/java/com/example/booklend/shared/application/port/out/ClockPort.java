package com.example.booklend.shared.application.port.out;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
