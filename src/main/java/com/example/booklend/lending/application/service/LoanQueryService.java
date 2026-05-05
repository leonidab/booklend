package com.example.booklend.lending.application.service;

import com.example.booklend.lending.application.port.in.LoanQueryUseCase;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.member.domain.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoanQueryService implements LoanQueryUseCase {

    private final LoadLoanPort loadLoanPort;

    public LoanQueryService(LoadLoanPort loadLoanPort) {
        this.loadLoanPort = loadLoanPort;
    }

    @Override
    public List<Loan> findActiveLoans(MemberId memberId) {
        return loadLoanPort.findActiveByMemberId(memberId);
    }

    @Override
    public List<Loan> findAllLoans(MemberId memberId) {
        return loadLoanPort.findAllByMemberId(memberId);
    }
}
