package com.example.booklend;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.application.port.in.ReserveBookUseCase;
import com.example.booklend.lending.application.port.in.ReturnBookUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookReturnedEventIT {

    @Autowired CatalogAdminUseCase catalogAdminUseCase;
    @Autowired MemberAdminUseCase memberAdminUseCase;
    @Autowired BorrowBookUseCase borrowBookUseCase;
    @Autowired ReturnBookUseCase returnBookUseCase;
    @Autowired ReserveBookUseCase reserveBookUseCase;
    @Autowired LoadReservationPort loadReservationPort;
    @Autowired OutboxEventJpaRepository outboxRepo;

    @Test
    void returningBook_withReservationQueue_publishesOutboxEvent_andKeepsReservation() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(outboxRepo.findByPublishedFalse())
                .anyMatch(e -> e.getEventType().equals("BookReadyForMemberEvent")
                        && e.getPayload().contains(waiter.getId().toString()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isPresent();
    }

    @Test
    void returningBook_withNoReservation_noOutboxEvent() {
        Book book = addBook();
        Member member = addMember("Alice");

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(outboxRepo.findByPublishedFalse()).isEmpty();
    }

    @Test
    void reservedMember_canBorrow_andReservationIsCleared() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(waiter.getId(), book.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isEmpty();
    }

    @Test
    void nonReservedMember_cannotBorrow_whenReservationExists() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member waiter = addMember("Bob");
        Member interloper = addMember("Carol");

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(waiter.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThatThrownBy(() -> borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(interloper.getId(), book.getId())))
                .isInstanceOf(BookReservedForOtherMemberException.class);
    }

    @Test
    void reservationQueue_isServedInOrder() {
        Book book = addBook();
        Member borrower = addMember("Alice");
        Member first = addMember("Bob");
        Member second = addMember("Carol");

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));
        reserveBookUseCase.reserve(new ReserveBookUseCase.ReserveCommand(first.getId(), book.getId()));
        reserveBookUseCase.reserve(new ReserveBookUseCase.ReserveCommand(second.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId()))
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.getMemberId()).isEqualTo(first.getId()));

        Loan secondLoan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(first.getId(), book.getId()));
        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(secondLoan.getId()));

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
