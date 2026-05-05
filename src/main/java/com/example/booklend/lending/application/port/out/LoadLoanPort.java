package com.example.booklend.lending.application.port.out;

import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.member.domain.MemberId;

import java.util.List;

public interface LoadLoanPort {
    Loan loadLoan(LoanId id);
    List<Loan> findActiveByMemberId(MemberId memberId);
    List<Loan> findAllByMemberId(MemberId memberId);
}
