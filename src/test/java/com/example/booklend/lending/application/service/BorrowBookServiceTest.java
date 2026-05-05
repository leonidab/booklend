package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.LoanPeriod;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.exception.OverdueLoanException;
import com.example.booklend.catalog.domain.exception.BookNotAvailableException;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.exception.MaxLoansExceededException;
import com.example.booklend.member.domain.exception.MemberRestrictedException;
import com.example.booklend.shared.infrastructure.inmemory.FakeClockAdapter;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryBookRepository;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryLoanRepository;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BorrowBookServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryMemberRepository memberRepo;
    private InMemoryBookRepository bookRepo;
    private InMemoryLoanRepository loanRepo;
    private FakeClockAdapter clock;
    private BorrowBookService service;

    @BeforeEach
    void setUp() {
        memberRepo = new InMemoryMemberRepository();
        bookRepo = new InMemoryBookRepository();
        loanRepo = new InMemoryLoanRepository();
        clock = new FakeClockAdapter(NOW);
        service = new BorrowBookService(memberRepo, memberRepo, bookRepo, bookRepo, loanRepo, loanRepo, clock);
    }

    private Member savedMember() {
        Member m = Member.create(MemberId.newId(), "Alice", "alice@test.com");
        memberRepo.save(m);
        return m;
    }

    private Book savedAvailableBook() {
        Book b = Book.create(BookId.newId(), new ISBN("9780201633610"), "Clean Code", "Martin");
        bookRepo.save(b);
        return b;
    }

    @Test
    void borrow_createsActiveLoan() {
        Member member = savedMember();
        Book book = savedAvailableBook();

        Loan loan = service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getMemberId()).isEqualTo(member.getId());
        assertThat(loan.getBookId()).isEqualTo(book.getId());
        assertThat(loan.getPeriod().dueDate()).isEqualTo(NOW.plus(LoanPeriod.LOAN_DAYS, ChronoUnit.DAYS));
    }

    @Test
    void borrow_marksBookUnavailable() {
        Member member = savedMember();
        Book book = savedAvailableBook();

        service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));

        assertThat(bookRepo.loadBook(book.getId()).isAvailable()).isFalse();
    }

    @Test
    void borrow_incrementsMemberLoanCount() {
        Member member = savedMember();
        Book book = savedAvailableBook();

        service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));

        assertThat(memberRepo.loadMember(member.getId()).getActiveLoansCount()).isEqualTo(1);
    }

    @Test
    void borrow_failsWhenBookUnavailable() {
        Member member = savedMember();
        Book book = Book.reconstitute(BookId.newId(), new ISBN("9780201633610"), "Title", "Author", false);
        bookRepo.save(book);

        assertThatThrownBy(() -> service.borrow(
                new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId())))
                .isInstanceOf(BookNotAvailableException.class);
    }

    @Test
    void borrow_failsWhenThreeActiveLoans() {
        Member member = savedMember();
        for (int i = 0; i < 3; i++) {
            Book book = savedAvailableBook();
            service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));
        }
        Book extra = savedAvailableBook();

        assertThatThrownBy(() -> service.borrow(
                new BorrowBookUseCase.BorrowCommand(member.getId(), extra.getId())))
                .isInstanceOf(MaxLoansExceededException.class);
    }

    @Test
    void borrow_failsWhenMemberRestricted() {
        Member member = savedMember();
        for (int i = 0; i < 3; i++) {
            Book b = savedAvailableBook();
            Loan loan = service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), b.getId()));
            loanRepo.saveLoan(lateReturnedLoan(loan));
            member = memberRepo.loadMember(member.getId());
            member.recordLoanReturned(true, NOW);
            memberRepo.save(member);
        }

        Member freshMember = memberRepo.loadMember(member.getId());
        Book extra = savedAvailableBook();
        assertThatThrownBy(() -> service.borrow(
                new BorrowBookUseCase.BorrowCommand(freshMember.getId(), extra.getId())))
                .isInstanceOf(MemberRestrictedException.class);
    }

    @Test
    void borrow_failsWhenMemberHasOverdueLoan() {
        Member member = savedMember();
        Book book1 = savedAvailableBook();
        service.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book1.getId()));

        clock.advanceTo(NOW.plus(20, ChronoUnit.DAYS));

        Book book2 = savedAvailableBook();
        assertThatThrownBy(() -> service.borrow(
                new BorrowBookUseCase.BorrowCommand(member.getId(), book2.getId())))
                .isInstanceOf(OverdueLoanException.class);
    }

    private Loan lateReturnedLoan(Loan original) {
        return Loan.reconstitute(
                original.getId(), original.getMemberId(), original.getBookId(),
                original.getPeriod(),
                original.getPeriod().dueDate().plus(5, ChronoUnit.DAYS),
                LoanStatus.RETURNED
        );
    }
}
