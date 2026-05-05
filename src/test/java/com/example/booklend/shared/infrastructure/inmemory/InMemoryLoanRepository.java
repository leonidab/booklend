package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.exception.LoanNotFoundException;
import com.example.booklend.member.domain.MemberId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLoanRepository implements LoadLoanPort, SaveLoanPort {

    private final Map<LoanId, Loan> store = new ConcurrentHashMap<>();

    @Override
    public Loan loadLoan(LoanId id) {
        Loan loan = store.get(id);
        if (loan == null) throw new LoanNotFoundException(id);
        return loan;
    }

    @Override
    public List<Loan> findActiveByMemberId(MemberId memberId) {
        return store.values().stream()
                .filter(l -> l.getMemberId().equals(memberId) && l.getStatus() == LoanStatus.ACTIVE)
                .toList();
    }

    @Override
    public List<Loan> findAllByMemberId(MemberId memberId) {
        return store.values().stream()
                .filter(l -> l.getMemberId().equals(memberId))
                .toList();
    }

    @Override
    public Loan saveLoan(Loan loan) { store.put(loan.getId(), loan); return loan; }
}
