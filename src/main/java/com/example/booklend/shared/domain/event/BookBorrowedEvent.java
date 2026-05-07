package com.example.booklend.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BookBorrowedEvent(
        UUID bookId,
        UUID memberId,
        UUID loanId,
        Instant occurredOn
) implements DomainEvent {}
