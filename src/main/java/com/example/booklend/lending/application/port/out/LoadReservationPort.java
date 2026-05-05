package com.example.booklend.lending.application.port.out;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.member.domain.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoadReservationPort {
    Optional<Reservation> findFirstByBookId(BookId bookId);
    Optional<Reservation> findByBookIdAndMemberId(BookId bookId, MemberId memberId);
    List<Reservation> findByMemberId(MemberId memberId);
}
