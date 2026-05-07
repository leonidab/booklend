package com.example.booklend.lending.application.service;

import com.example.booklend.lending.application.port.in.BorrowBookUseCase;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.shared.application.port.out.ClockPort;
import com.example.booklend.shared.application.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BorrowBookService implements BorrowBookUseCase {

    private final LoadLoanPort loadLoanPort;
    private final SaveLoanPort saveLoanPort;
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clock;

    @Override
    public Loan borrow(BorrowCommand command) {
        Instant now = clock.now();

        Loan.assertNoOverdue(
                loadLoanPort.findActiveByMemberId(command.memberId()), now, command.memberId());

        Optional<Reservation> firstReservation = loadReservationPort.findFirstByBookId(command.bookId());
        firstReservation.ifPresent(r -> r.assertClaimableBy(command.memberId()));

        Loan loan = Loan.create(LoanId.newId(), command.memberId(), command.bookId(), now);
        saveLoanPort.saveLoan(loan);
        firstReservation.ifPresent(r -> saveReservationPort.deleteReservation(r.getId()));

        loan.pullDomainEvents().forEach(eventPublisher::publish);

        return loan;
    }
}
