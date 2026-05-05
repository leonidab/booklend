package com.example.booklend.lending.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private Instant requestedAt;

    protected ReservationJpaEntity() {}

    public ReservationJpaEntity(UUID id, UUID bookId, UUID memberId, Instant requestedAt) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.requestedAt = requestedAt;
    }

    public UUID getId() { return id; }
    public UUID getBookId() { return bookId; }
    public UUID getMemberId() { return memberId; }
    public Instant getRequestedAt() { return requestedAt; }
}
