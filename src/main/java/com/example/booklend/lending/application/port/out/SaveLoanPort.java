package com.example.booklend.lending.application.port.out;

import com.example.booklend.lending.domain.Loan;

public interface SaveLoanPort {
    void saveLoan(Loan loan);
}
