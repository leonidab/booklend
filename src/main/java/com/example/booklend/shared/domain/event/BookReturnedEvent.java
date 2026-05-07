package com.example.booklend.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BookReturnedEvent(
        UUID bookId,
        UUID memberId,
        UUID loanId,
        boolean wasLate,
        Instant occurredOn
) implements DomainEvent {}
