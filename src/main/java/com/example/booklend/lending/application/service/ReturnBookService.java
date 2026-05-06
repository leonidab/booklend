package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.lending.application.port.in.ReturnBookUseCase;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.event.BookReadyForMemberEvent;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.shared.application.port.out.ClockPort;
import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReturnBookService implements ReturnBookUseCase {

    private final LoadLoanPort loadLoanPort;
    private final SaveLoanPort saveLoanPort;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final LoadBookPort loadBookPort;
    private final SaveBookPort saveBookPort;
    private final LoadReservationPort loadReservationPort;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clock;

    public ReturnBookService(LoadLoanPort loadLoanPort, SaveLoanPort saveLoanPort,
                             LoadMemberPort loadMemberPort, SaveMemberPort saveMemberPort,
                             LoadBookPort loadBookPort, SaveBookPort saveBookPort,
                             LoadReservationPort loadReservationPort,
                             DomainEventPublisher eventPublisher, ClockPort clock) {
        this.loadLoanPort = loadLoanPort;
        this.saveLoanPort = saveLoanPort;
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.loadBookPort = loadBookPort;
        this.saveBookPort = saveBookPort;
        this.loadReservationPort = loadReservationPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public Loan returnBook(ReturnCommand command) {
        Loan loan = loadLoanPort.loadLoan(command.loanId());
        Member member = loadMemberPort.loadMember(loan.getMemberId());
        Book book = loadBookPort.loadBook(loan.getBookId());

        Instant now = clock.now();
        loan.returnLoan(now);
        member.recordLoanReturned(loan.wasReturnedLate());
        book.markAvailable();

        saveLoanPort.saveLoan(loan);
        saveMemberPort.saveMember(member);
        saveBookPort.saveBook(book);

        loadReservationPort.findFirstByBookId(book.getId())
                .ifPresent(r -> loan.registerEvent(
                        new BookReadyForMemberEvent(book.getId(), r.getMemberId(), now)));

        loan.pullDomainEvents().forEach(eventPublisher::publish);

        return loan;
    }
}
