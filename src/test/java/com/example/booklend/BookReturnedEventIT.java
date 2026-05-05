package com.example.booklend;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.application.port.in.ReserveBookUseCase;
import com.example.booklend.lending.application.port.in.ReturnBookUseCase;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BookReturnedEvent flows end-to-end:
 * return → event published → handler notified → reservation deleted.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookReturnedEventIT {

    @Autowired CatalogAdminUseCase catalogAdminUseCase;
    @Autowired MemberAdminUseCase memberAdminUseCase;
    @Autowired BorrowBookUseCase borrowBookUseCase;
    @Autowired ReturnBookUseCase returnBookUseCase;
    @Autowired ReserveBookUseCase reserveBookUseCase;
    @Autowired LoadReservationPort loadReservationPort;

    @Test
    void returningBook_notifiesFirstReservation_andRemovesIt() {
        Book book = catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN("9780201633610"), "Clean Code", "Martin"));
        Member borrower = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Alice", "alice@test.com"));
        Member waiter = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Bob", "bob@test.com"));

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));

        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(waiter.getId(), book.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isPresent();

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        // Handler consumed the reservation upon BookReturnedEvent
        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isEmpty();
    }

    @Test
    void returningBook_withNoReservation_completesWithoutError() {
        Book book = catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN("9780201633610"), "Clean Code", "Martin"));
        Member member = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Alice", "alice@test.com"));

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(member.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        assertThat(loadReservationPort.findFirstByBookId(book.getId())).isEmpty();
    }

    @Test
    void reservationQueue_isServedInOrder() {
        Book book = catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN("9780201633610"), "Clean Code", "Martin"));
        Member borrower = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Alice", "alice@test.com"));
        Member first = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Bob", "bob@test.com"));
        Member second = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Carol", "carol@test.com"));

        Loan loan = borrowBookUseCase.borrow(
                new BorrowBookUseCase.BorrowCommand(borrower.getId(), book.getId()));

        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(first.getId(), book.getId()));
        reserveBookUseCase.reserve(
                new ReserveBookUseCase.ReserveCommand(second.getId(), book.getId()));

        returnBookUseCase.returnBook(new ReturnBookUseCase.ReturnCommand(loan.getId()));

        // First reservation consumed; second still in queue
        assertThat(loadReservationPort.findFirstByBookId(book.getId()))
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.getMemberId()).isEqualTo(second.getId()));
    }
}
