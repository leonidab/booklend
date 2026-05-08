package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.application.service.BookLoanEventHandler;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowUseCase;
import com.example.booklend.lending.application.port.in.ReturnUseCase;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.lending.domain.event.BookReadyForMemberEvent;
import com.example.booklend.member.application.service.MemberLoanEventHandler;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.MemberStatus;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
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

class ReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryMemberRepository memberRepo;
    private InMemoryBookRepository bookRepo;
    private InMemoryLoanRepository loanRepo;
    private InMemoryReservationRepository reservationRepo;
    private InMemoryDomainEventPublisher eventPublisher;
    private FakeClockAdapter clock;
    private LendingService borrowService;
    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        memberRepo = new InMemoryMemberRepository();
        bookRepo = new InMemoryBookRepository();
        loanRepo = new InMemoryLoanRepository();
        reservationRepo = new InMemoryReservationRepository();
        eventPublisher = new InMemoryDomainEventPublisher();
        clock = new FakeClockAdapter(NOW);

        MemberLoanEventHandler memberHandler = new MemberLoanEventHandler(memberRepo, memberRepo);
        BookLoanEventHandler bookHandler = new BookLoanEventHandler(bookRepo, bookRepo);
        eventPublisher.register(BookBorrowedEvent.class, bookHandler::onBookBorrowed);
        eventPublisher.register(BookBorrowedEvent.class, memberHandler::onBookBorrowed);
        eventPublisher.register(BookReturnedEvent.class, bookHandler::onBookReturned);
        eventPublisher.register(BookReturnedEvent.class, memberHandler::onBookReturned);

        borrowService = new LendingService(loanRepo, loanRepo, reservationRepo, reservationRepo,
                eventPublisher, clock);
        returnService = new ReturnService(loanRepo, loanRepo, reservationRepo, eventPublisher, clock);
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
        Loan loan = borrowService.borrow(new BorrowUseCase.BorrowCommand(member.getId(), book.getId()));
        eventPublisher.clear();
        return loan;
    }

    @Test
    void returnBook_setsLoanStatusToReturned() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        Loan returned = returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    void returnBook_makesBookAvailableAgain() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(bookRepo.loadBook(book.getId()).isAvailable()).isTrue();
    }

    @Test
    void returnBook_decrementsActiveLoanCount() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(memberRepo.loadMember(member.getId()).getActiveLoansCount()).isZero();
    }

    @Test
    void returnBook_publishesBookReturnedEvent() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(eventPublisher.getPublished()).hasAtLeastOneElementOfType(BookReturnedEvent.class);
    }

    @Test
    void returnBook_noBookReadyForMemberEvent_whenNoReservationQueue() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(eventPublisher.getPublished())
                .noneMatch(e -> e instanceof BookReadyForMemberEvent);
    }

    @Test
    void returnBook_publishesBookReadyForMemberEvent_whenReservationExists() {
        Member borrower = savedMember();
        Member waiter = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(borrower, book);

        reservationRepo.saveReservation(Reservation.create(
                ReservationId.newId(), book.getId(), waiter.getId(), NOW));

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        List<DomainEvent> events = eventPublisher.getPublished();
        assertThat(events).hasAtLeastOneElementOfType(BookReadyForMemberEvent.class);
        BookReadyForMemberEvent event = events.stream()
                .filter(e -> e instanceof BookReadyForMemberEvent)
                .map(e -> (BookReadyForMemberEvent) e)
                .findFirst().orElseThrow();
        assertThat(event.bookId()).isEqualTo(book.getId());
        assertThat(event.memberId()).isEqualTo(waiter.getId());
    }

    @Test
    void returnBook_keepsReservation_forExclusiveBorrow() {
        Member borrower = savedMember();
        Member waiter = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(borrower, book);

        reservationRepo.saveReservation(Reservation.create(
                ReservationId.newId(), book.getId(), waiter.getId(), NOW));

        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(reservationRepo.findFirstByBookId(book.getId())).isPresent();
    }

    @Test
    void lateReturn_incrementsLateReturnCount() {
        Member member = savedMember();
        Book book = savedBook();
        Loan loan = borrowBook(member, book);

        clock.advanceTo(NOW.plus(20, ChronoUnit.DAYS));
        returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(memberRepo.loadMember(member.getId()).getLateReturnCount()).isEqualTo(1);
    }

    @Test
    void thirdLateReturn_restrictsMember() {
        Member member = savedMember();

        for (int i = 0; i < 3; i++) {
            clock.advanceTo(NOW);
            Book book = savedBook();
            Loan loan = borrowBook(member, book);
            clock.advanceTo(NOW.plus(20, ChronoUnit.DAYS));
            returnService.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));
            member = memberRepo.loadMember(member.getId());
        }

        assertThat(member.getStatus()).isEqualTo(MemberStatus.RESTRICTED);
    }
}
