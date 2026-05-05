package com.example.booklend.lending.domain.exception;

import com.example.booklend.lending.domain.LoanId;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(LoanId loanId) {
        super("Loan not found: " + loanId);
    }
}
