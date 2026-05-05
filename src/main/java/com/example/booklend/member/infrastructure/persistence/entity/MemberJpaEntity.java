package com.example.booklend.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "members")
public class MemberJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int activeLoansCount;

    @Column(nullable = false)
    private int lateReturnCount;

    protected MemberJpaEntity() {}

    public MemberJpaEntity(UUID id, String name, String email, String status,
                           int activeLoansCount, int lateReturnCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.activeLoansCount = activeLoansCount;
        this.lateReturnCount = lateReturnCount;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public int getActiveLoansCount() { return activeLoansCount; }
    public int getLateReturnCount() { return lateReturnCount; }
    public void setStatus(String status) { this.status = status; }
    public void setActiveLoansCount(int activeLoansCount) { this.activeLoansCount = activeLoansCount; }
    public void setLateReturnCount(int lateReturnCount) { this.lateReturnCount = lateReturnCount; }
}
