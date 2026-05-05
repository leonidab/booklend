package com.example.booklend.lending.domain;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.domain.event.BookReturnedEvent;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Loan {

    private final LoanId id;
    private final MemberId memberId;
    private final BookId bookId;
    private final LoanPeriod period;
    private Instant returnedAt;
    private LoanStatus status;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

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
        return new Loan(id, memberId, bookId, LoanPeriod.startingFrom(borrowedAt), null, LoanStatus.ACTIVE);
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
        domainEvents.add(new BookReturnedEvent(bookId, memberId, returnedAt));
    }

    public boolean isOverdue(Instant now) {
        return status == LoanStatus.ACTIVE && period.isOverdue(now);
    }

    public boolean wasReturnedLate() {
        return status == LoanStatus.RETURNED && period.wasLate(returnedAt);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public LoanId getId() { return id; }
    public MemberId getMemberId() { return memberId; }
    public BookId getBookId() { return bookId; }
    public LoanPeriod getPeriod() { return period; }
    public Instant getReturnedAt() { return returnedAt; }
    public LoanStatus getStatus() { return status; }
}
