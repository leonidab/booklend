package com.example.booklend.lending.domain.event;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.domain.event.DomainEvent;

import java.time.Instant;

public record BookReadyForMemberEvent(
        BookId bookId,
        MemberId memberId,
        Instant occurredOn
) implements DomainEvent {}
