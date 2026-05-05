package com.example.booklend.lending.domain;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.member.domain.MemberId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private Loan newLoan() {
        return Loan.create(LoanId.newId(), MemberId.newId(), BookId.newId(), NOW);
    }

    @Test
    void newLoan_isActive_withDueDateFourteenDaysLater() {
        Loan loan = newLoan();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getPeriod().dueDate())
                .isEqualTo(NOW.plus(LoanPeriod.LOAN_DAYS, ChronoUnit.DAYS));
    }

    @Test
    void loanNotOverdue_beforeDueDate() {
        Loan loan = newLoan();
        Instant beforeDue = NOW.plus(13, ChronoUnit.DAYS);
        assertThat(loan.isOverdue(beforeDue)).isFalse();
    }

    @Test
    void loanOverdue_afterDueDate() {
        Loan loan = newLoan();
        Instant afterDue = NOW.plus(15, ChronoUnit.DAYS);
        assertThat(loan.isOverdue(afterDue)).isTrue();
    }

    @Test
    void returnLoan_setsStatusReturned() {
        Loan loan = newLoan();
        Instant returnedAt = NOW.plus(5, ChronoUnit.DAYS);
        loan.returnLoan(returnedAt);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.getReturnedAt()).isEqualTo(returnedAt);
    }

    @Test
    void returnedOnTime_notLate() {
        Loan loan = newLoan();
        loan.returnLoan(NOW.plus(10, ChronoUnit.DAYS));
        assertThat(loan.wasReturnedLate()).isFalse();
    }

    @Test
    void returnedAfterDueDate_isLate() {
        Loan loan = newLoan();
        loan.returnLoan(NOW.plus(20, ChronoUnit.DAYS));
        assertThat(loan.wasReturnedLate()).isTrue();
    }

    @Test
    void returningAlreadyReturnedLoan_throws() {
        Loan loan = newLoan();
        loan.returnLoan(NOW.plus(1, ChronoUnit.DAYS));
        assertThatThrownBy(() -> loan.returnLoan(NOW.plus(2, ChronoUnit.DAYS)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnedLoan_notOverdue_regardlessOfTime() {
        Loan loan = newLoan();
        loan.returnLoan(NOW.plus(1, ChronoUnit.DAYS));
        assertThat(loan.isOverdue(NOW.plus(20, ChronoUnit.DAYS))).isFalse();
    }
}
