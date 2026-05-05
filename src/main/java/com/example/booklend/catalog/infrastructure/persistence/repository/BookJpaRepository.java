package com.example.booklend.catalog.infrastructure.persistence.repository;

import com.example.booklend.catalog.infrastructure.persistence.entity.BookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookJpaRepository extends JpaRepository<BookJpaEntity, UUID> {}
