package com.example.booklend.shared.application.port.out;

import com.example.booklend.shared.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
