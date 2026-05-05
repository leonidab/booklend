package com.example.booklend.lending.infrastructure.persistence.repository;

import com.example.booklend.lending.infrastructure.persistence.entity.LoanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanJpaRepository extends JpaRepository<LoanJpaEntity, UUID> {
    List<LoanJpaEntity> findByMemberIdAndStatus(UUID memberId, String status);
    List<LoanJpaEntity> findByMemberId(UUID memberId);
}
