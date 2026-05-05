package com.example.booklend.lending.application.port.in;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.member.domain.MemberId;

public interface BorrowBookUseCase {

    record BorrowCommand(MemberId memberId, BookId bookId) {}

    Loan borrow(BorrowCommand command);
}
