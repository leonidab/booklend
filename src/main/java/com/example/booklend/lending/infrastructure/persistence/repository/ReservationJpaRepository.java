package com.example.booklend.lending.infrastructure.persistence.repository;

import com.example.booklend.lending.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {
    List<ReservationJpaEntity> findByBookIdOrderByRequestedAtAsc(UUID bookId);
    Optional<ReservationJpaEntity> findByBookIdAndMemberId(UUID bookId, UUID memberId);
    List<ReservationJpaEntity> findByMemberId(UUID memberId);
}
