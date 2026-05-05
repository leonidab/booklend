package com.example.booklend.lending.application.port.in;

import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;

public interface ReturnBookUseCase {

    record ReturnCommand(LoanId loanId) {}

    Loan returnBook(ReturnCommand command);
}
