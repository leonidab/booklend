package com.example.booklend.lending.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Setter
    private Instant returnedAt;

    @Setter
    @Column(nullable = false)
    private String status;
}
