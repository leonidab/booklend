package com.example.booklend.shared.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
