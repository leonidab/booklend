package com.example.booklend.shared.infrastructure.event;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.event.BookReturnedEvent;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.booklend.shared.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    @Mock
    private OutboxEventJpaRepository repository;

    private OutboxDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxDomainEventPublisher(repository, new ObjectMapper());
    }

    @Test
    void publish_savesRowWithCorrectEventType() {
        publisher.publish(bookReturnedEvent());

        OutboxEventJpaEntity saved = capturesaved();
        assertThat(saved.getEventType()).isEqualTo("BookReturnedEvent");
    }

    @Test
    void publish_savedRowIsUnpublished() {
        publisher.publish(bookReturnedEvent());

        assertThat(capturesaved().isPublished()).isFalse();
    }

    @Test
    void publish_payloadContainsEventData() {
        BookId bookId = BookId.newId();
        MemberId memberId = MemberId.newId();

        publisher.publish(new BookReturnedEvent(bookId, memberId, Instant.now()));

        String payload = capturesaved().getPayload();
        assertThat(payload).contains(bookId.value().toString());
        assertThat(payload).contains(memberId.value().toString());
    }

    @Test
    void publish_rowHasNonNullId() {
        publisher.publish(bookReturnedEvent());

        assertThat(capturesaved().getId()).isNotNull();
    }

    @Test
    void publish_rowHasCreatedAt() {
        publisher.publish(bookReturnedEvent());

        assertThat(capturesaved().getCreatedAt()).isNotNull();
    }

    private OutboxEventJpaEntity capturesaved() {
        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private BookReturnedEvent bookReturnedEvent() {
        return new BookReturnedEvent(BookId.newId(), MemberId.newId(), Instant.now());
    }
}
