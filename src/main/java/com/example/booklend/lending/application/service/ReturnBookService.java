package com.example.booklend.lending.application.service;

import com.example.booklend.lending.application.port.in.ReturnBookUseCase;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.event.BookReadyForMemberEvent;
import com.example.booklend.shared.application.port.out.ClockPort;
import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class ReturnBookService implements ReturnBookUseCase {

    private final LoadLoanPort loadLoanPort;
    private final SaveLoanPort saveLoanPort;
    private final LoadReservationPort loadReservationPort;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clock;

    @Override
    public Loan returnBook(ReturnCommand command) {
        Loan loan = loadLoanPort.loadLoan(command.loanId());

        Instant now = clock.now();
        loan.returnLoan(now);
        saveLoanPort.saveLoan(loan);

        loadReservationPort.findFirstByBookId(loan.getBookId())
                .ifPresent(r -> loan.registerEvent(
                        new BookReadyForMemberEvent(loan.getBookId(), r.getMemberId(), now)));

        loan.pullDomainEvents().forEach(eventPublisher::publish);

        return loan;
    }
}
