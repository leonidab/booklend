package com.example.booklend.shared.infrastructure.event;

import com.example.booklend.shared.application.port.out.ClockPort;
import com.example.booklend.shared.domain.event.DomainEvent;
import com.example.booklend.shared.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.booklend.shared.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final ClockPort clock;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxEventJpaEntity(
                    UUID.randomUUID(),
                    event.getClass().getSimpleName(),
                    payload,
                    clock.now()
            ));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
