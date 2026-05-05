package com.example.booklend.lending.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record LoanPeriod(Instant borrowedAt, Instant dueDate) {

    public static final int LOAN_DAYS = 14;

    public static LoanPeriod startingFrom(Instant borrowedAt) {
        return new LoanPeriod(borrowedAt, borrowedAt.plus(LOAN_DAYS, ChronoUnit.DAYS));
    }

    public boolean isOverdue(Instant now) {
        return now.isAfter(dueDate);
    }

    public boolean wasLate(Instant returnedAt) {
        return returnedAt.isAfter(dueDate);
    }
}
