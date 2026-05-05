package com.example.booklend.shared.infrastructure.clock;

import com.example.booklend.shared.application.port.out.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
