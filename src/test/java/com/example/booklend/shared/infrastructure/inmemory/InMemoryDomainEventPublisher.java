package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import com.example.booklend.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InMemoryDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();
    private final Map<Class<? extends DomainEvent>, List<Consumer<? extends DomainEvent>>> handlers = new HashMap<>();

    public <E extends DomainEvent> void register(Class<E> eventType, Consumer<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void publish(DomainEvent event) {
        published.add(event);
        List<Consumer<? extends DomainEvent>> consumers = handlers.get(event.getClass());
        if (consumers != null) {
            for (Consumer consumer : consumers) {
                consumer.accept(event);
            }
        }
    }

    public List<DomainEvent> getPublished() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
