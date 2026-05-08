package com.example.booklend.lending.application.port.in;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.member.domain.MemberId;

public interface ReserveUseCase {

    record ReserveCommand(MemberId memberId, BookId bookId) {}

    Reservation reserve(ReserveCommand command);
}
