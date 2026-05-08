package com.example.booklend;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowUseCase;
import com.example.booklend.lending.application.port.in.ReserveUseCase;
import com.example.booklend.lending.application.port.in.ReturnUseCase;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.exception.BookReservedForOtherMemberException;
import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.domain.Member;
import com.example.booklend.shared.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("it")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookReturnedEventIT {

    @Autowired CatalogAdminUseCase catalogAdminUseCase;
    @Autowired MemberAdminUseCase memberAdminUseCase;
    @Autowired
    BorrowUseCase borrowUseCase;
    @Autowired
    ReturnUseCase returnUseCase;
    @Autowired
    ReserveUseCase reserveUseCase;
    @Autowired LoadReservationPort loadReservationPort;
    @Autowired OutboxEventJpaRepository outboxRepo;

    @Test
    void returningBook_withReservationQueue_publishesOutboxEvent_andKeepsReservation() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");

        Loan loan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveUseCase.reserve(
                new ReserveUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(outboxRepo.findByPublishedFalse())
                .anyMatch(e -> e.getEventType().equals("BookReadyForMemberEvent")
                        && e.getPayload().contains(waiter.getId().toString()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isPresent();
    }

    @Test
    void returningBook_withNoReservation_noBookReadyForMemberEvent() {
        Book book = addBook();
        Member member = addMember("Alice");

        Loan loan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(member.getId(), book.getId()));

        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(outboxRepo.findByPublishedFalse())
                .noneMatch(e -> e.getEventType().equals("BookReadyForMemberEvent"));
    }

    @Test
    void reservedMember_canBorrow_andReservationIsCleared() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");

        Loan loan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveUseCase.reserve(
                new ReserveUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(waiter.getId(), book.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isEmpty();
    }

    @Test
    void nonReservedMember_cannotBorrow_whenReservationExists() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");
        Member interloper = addMember("Carol");

        Loan loan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveUseCase.reserve(
                new ReserveUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThatThrownBy(() -> borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(interloper.getId(), book.getId())))
                .isInstanceOf(BookReservedForOtherMemberException.class);
    }

    @Test
    void reservationQueue_isServedInOrder() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member first = addMember("Bob");
        Member second = addMember("Carol");

        Loan loan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveUseCase.reserve(new ReserveUseCase.ReserveCommand(first.getId(), book.getId()));
        reserveUseCase.reserve(new ReserveUseCase.ReserveCommand(second.getId(), book.getId()));

        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(loan.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId()))
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.getMemberId()).isEqualTo(first.getId()));

        Loan secondLoan = borrowUseCase.borrow(
                new BorrowUseCase.BorrowCommand(first.getId(), book.getId()));
        returnUseCase.returnBook(new ReturnUseCase.ReturnCommand(secondLoan.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId()))
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.getMemberId()).isEqualTo(second.getId()));
    }

    private Book addBook() {
        return catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN("9780201633610"), "Clean Code", "Martin"));
    }

    private Member addMember(String name) {
        return memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand(name, name.toLowerCase() + "@test.com"));
    }
}
