package com.example.booklend.lending.domain;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.exception.OverdueLoanException;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.domain.AggregateRoot;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.Collection;

@Getter
public class Loan extends AggregateRoot {

    private final LoanId id;
    private final MemberId memberId;
    private final BookId bookId;
    private final LoanPeriod period;
    private Instant returnedAt;
    private LoanStatus status;

    private Loan(LoanId id, MemberId memberId, BookId bookId,
                 LoanPeriod period, Instant returnedAt, LoanStatus status) {
        this.id = id;
        this.memberId = memberId;
        this.bookId = bookId;
        this.period = period;
        this.returnedAt = returnedAt;
        this.status = status;
    }

    public static Loan create(LoanId id, MemberId memberId, BookId bookId, Instant borrowedAt) {
        Loan loan = new Loan(id, memberId, bookId, LoanPeriod.startingFrom(borrowedAt), null, LoanStatus.ACTIVE);
        loan.registerEvent(new BookBorrowedEvent(
                bookId.value(), memberId.value(), id.value(), borrowedAt));
        return loan;
    }

    public static Loan reconstitute(LoanId id, MemberId memberId, BookId bookId,
                                    LoanPeriod period, Instant returnedAt, LoanStatus status) {
        return new Loan(id, memberId, bookId, period, returnedAt, status);
    }

    public void returnLoan(Instant returnedAt) {
        if (status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan " + id + " is not active");
        }
        this.returnedAt = returnedAt;
        this.status = LoanStatus.RETURNED;
        registerEvent(new BookReturnedEvent(
                bookId.value(), memberId.value(), id.value(), wasReturnedLate(), returnedAt));
    }

    public boolean isOverdue(Instant now) {
        return status == LoanStatus.ACTIVE && period.isOverdue(now);
    }

    public boolean wasReturnedLate() {
        return status == LoanStatus.RETURNED && period.wasLate(returnedAt);
    }

    public static void assertNoOverdue(Collection<Loan> loans, Instant now, MemberId memberId) {
        if (loans.stream().anyMatch(loan -> loan.isOverdue(now))) {
            throw new OverdueLoanException(memberId);
        }
    }
}
