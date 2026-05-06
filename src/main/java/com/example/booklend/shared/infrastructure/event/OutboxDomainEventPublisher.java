package com.example.booklend.shared.infrastructure.event;

import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import com.example.booklend.shared.domain.event.DomainEvent;
import com.example.booklend.shared.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.booklend.shared.infrastructure.persistence.repository.OutboxEventJpaRepository;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxEventJpaEntity(
                    UUID.randomUUID(),
                    event.getClass().getSimpleName(),
                    payload,
                    Instant.now()
            ));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
