package com.example.booklend.lending.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loans")
public class LoanJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private Instant borrowedAt;

    @Column(nullable = false)
    private Instant dueDate;

    private Instant returnedAt;

    @Column(nullable = false)
    private String status;

    protected LoanJpaEntity() {}

    public LoanJpaEntity(UUID id, UUID memberId, UUID bookId,
                         Instant borrowedAt, Instant dueDate, Instant returnedAt, String status) {
        this.id = id;
        this.memberId = memberId;
        this.bookId = bookId;
        this.borrowedAt = borrowedAt;
        this.dueDate = dueDate;
        this.returnedAt = returnedAt;
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getMemberId() { return memberId; }
    public UUID getBookId() { return bookId; }
    public Instant getBorrowedAt() { return borrowedAt; }
    public Instant getDueDate() { return dueDate; }
    public Instant getReturnedAt() { return returnedAt; }
    public String getStatus() { return status; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public void setStatus(String status) { this.status = status; }
}
