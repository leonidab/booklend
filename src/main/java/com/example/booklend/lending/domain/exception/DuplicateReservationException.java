package com.example.booklend.lending.domain.exception;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.member.domain.MemberId;

public class DuplicateReservationException extends RuntimeException {
    public DuplicateReservationException(BookId bookId, MemberId memberId) {
        super("Member " + memberId + " already has a reservation for book " + bookId);
    }
}
