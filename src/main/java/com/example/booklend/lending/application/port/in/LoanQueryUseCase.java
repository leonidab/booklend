package com.example.booklend.lending.application.port.in;

import com.example.booklend.lending.domain.Loan;
import com.example.booklend.member.domain.MemberId;

import java.util.List;

public interface LoanQueryUseCase {
    List<Loan> findActiveLoans(MemberId memberId);
    List<Loan> findAllLoans(MemberId memberId);
}
