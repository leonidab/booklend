package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.exception.BookReservedForOtherMemberException;
import com.example.booklend.lending.domain.exception.OverdueLoanException;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.shared.application.port.out.ClockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class BorrowBookService implements BorrowBookUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final LoadBookPort loadBookPort;
    private final SaveBookPort saveBookPort;
    private final LoadLoanPort loadLoanPort;
    private final SaveLoanPort saveLoanPort;
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final ClockPort clock;

    public BorrowBookService(LoadMemberPort loadMemberPort, SaveMemberPort saveMemberPort,
                             LoadBookPort loadBookPort, SaveBookPort saveBookPort,
                             LoadLoanPort loadLoanPort, SaveLoanPort saveLoanPort,
                             LoadReservationPort loadReservationPort, SaveReservationPort saveReservationPort,
                             ClockPort clock) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.loadBookPort = loadBookPort;
        this.saveBookPort = saveBookPort;
        this.loadLoanPort = loadLoanPort;
        this.saveLoanPort = saveLoanPort;
        this.loadReservationPort = loadReservationPort;
        this.saveReservationPort = saveReservationPort;
        this.clock = clock;
    }

    @Override
    public Loan borrow(BorrowCommand command) {
        Member member = loadMemberPort.loadMember(command.memberId());
        member.assertCanBorrow();

        Instant now = clock.now();
        boolean hasOverdue = loadLoanPort.findActiveByMemberId(command.memberId())
                .stream()
                .anyMatch(loan -> loan.isOverdue(now));
        if (hasOverdue) {
            throw new OverdueLoanException(command.memberId());
        }

        Book book = loadBookPort.loadBook(command.bookId());
        book.checkAvailable();

        Reservation firstReservation = loadReservationPort.findFirstByBookId(command.bookId())
                .orElse(null);
        if (firstReservation != null && !firstReservation.getMemberId().equals(command.memberId())) {
            throw new BookReservedForOtherMemberException(command.bookId(), firstReservation.getMemberId());
        }

        Loan loan = Loan.create(LoanId.newId(), command.memberId(), command.bookId(), now);
        member.recordLoanTaken();
        book.markUnavailable();

        saveLoanPort.saveLoan(loan);
        saveMemberPort.saveMember(member);
        saveBookPort.saveBook(book);

        if (firstReservation != null) {
            saveReservationPort.deleteReservation(firstReservation.getId());
        }

        return loan;
    }
}
