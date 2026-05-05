package com.example.booklend.member.domain.event;

import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.domain.event.DomainEvent;

import java.time.Instant;

public record MemberRestrictedEvent(
        MemberId memberId,
        Instant occurredOn
) implements DomainEvent {}
