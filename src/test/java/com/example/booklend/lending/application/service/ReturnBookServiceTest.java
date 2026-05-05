package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.application.port.in.ReturnBookUseCase;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.event.BookReturnedEvent;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.MemberStatus;
import com.example.booklend.member.domain.event.MemberRestrictedEvent;
import com.example.booklend.shared.domain.event.DomainEvent;
import com.example.booklend.shared.infrastructure.inmemory.FakeClockAdapter;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryBookRepository;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryDomainEventPublisher;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryLoanRepository;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryMemberRepository;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnBookServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryMemberRepository memberRepo;
    private InMemoryBookRepository bookRepo;
    private InMemoryLoanRepository loanRepo;
    private InMemoryReservationRepository reservationRepo;
    private InMemoryDomainEventPublisher eventPublisher;
    private FakeClockAdapter clock;
    private BorrowBookService borrowService;
    private ReturnBookService returnService;

    @BeforeEach
    void setUp() {
        memberRepo = new InMemoryMemberRepository();
        bookRepo = new InMemoryBookRepository();
        loanRepo = new InMemoryLoanRepository();
        reservationRepo = new InMemoryReservationRepository();
        eventPublisher = new InMemoryDomainEventPublisher();
        clock = new FakeClockAdapter(NOW);
        borrowService = new BorrowBookService(memberRepo, memberRepo, bookRepo, bookRepo, loanRepo, loanRepo, clock);
        returnService = new ReturnBookService(loanRepo, loanRepo, memberRepo, memberRepo, bookRepo, bookRepo, reservationRepo, reservationRepo, eventPublisher, clock);
    }

    private Member savedMember() {
        Member m = Member.create(MemberId.newId(), "Bob", "bob@test.com");
        memberRepo.save(m);
        return m;
    }

    private Book savedBook() {
        Book b = Book.create(BookId.newId(), new ISBN("9780201633610"), "Clean Code", "Martin");
        bookRepo.save(b);
        return b;
    }

    private Loan borrowBook(Member member, Book book) {
        return borrowService.borrow(new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));
    }

    @Test
    void returnBook_setsLoanStatusToReturned() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        Loan returned = returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    void returnBook_makesBookAvailableAgain() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(bookRepo.loadBook(book.getId()).isAvailable()).isTrue();
    }

    @Test
    void returnBook_decrementsActiveLoanCount() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(memberRepo.loadMember(member.getId()).getActiveLoansCount()).isZero();
    }

    @Test
    void returnBook_publishesBookReturnedEvent() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        List<DomainEvent> events = eventPublisher.getPublished();
        assertThat(events).hasAtLeastOneElementOfType(BookReturnedEvent.class);
        BookReturnedEvent event = events.stream()
                .filter(e -> e instanceof BookReturnedEvent)
                .map(e -> (BookReturnedEvent) e)
                .findFirst().orElseThrow();
        assertThat(event.bookId()).isEqualTo(book.getId());
        assertThat(event.memberId()).isEqualTo(member.getId());
    }

    @Test
    void lateReturn_incrementsLateReturnCount() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        clock.advanceTo(NOW.plus(20, ChronoUnit.DAYS));
        returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(memberRepo.loadMember(member.getId()).getLateReturnCount()).isEqualTo(1);
    }

    @Test
    void thirdLateReturn_restrictseMember_andPublishesRestrictedEvent() {
        Member member = savedMember();

        for (int i = 0; i < 3; i++) {
            clock.advanceTo(NOW);
            Book book = savedBook();
            Loan loan = borrowBook(member, book);
            clock.advanceTo(NOW.plus(20, ChronoUnit.DAYS));
            eventPublisher.clear();
            returnService.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));
            member = memberRepo.loadMember(member.getId());
        }

        assertThat(member.getStatus()).isEqualTo(MemberStatus.RESTRICTED);

        MemberId restrictedId = member.getId();
        List<DomainEvent> lastEvents = eventPublisher.getPublished();
        assertThat(lastEvents).anyMatch(e -> e instanceof MemberRestrictedEvent evt
                && evt.memberId().equals(restrictedId));
    }
}
