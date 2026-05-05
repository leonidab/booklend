package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import com.example.booklend.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public class InMemoryDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        published.add(event);
    }

    public List<DomainEvent> getPublished() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
