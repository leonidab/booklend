package com.example.booklend.shared.infrastructure.web;

import com.example.booklend.catalog.domain.exception.BookNotAvailableException;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;
import com.example.booklend.lending.domain.exception.DuplicateReservationException;
import com.example.booklend.lending.domain.exception.LoanNotFoundException;
import com.example.booklend.lending.domain.exception.OverdueLoanException;
import com.example.booklend.member.domain.exception.MaxLoansExceededException;
import com.example.booklend.member.domain.exception.MemberNotFoundException;
import com.example.booklend.member.domain.exception.MemberRestrictedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class, MemberNotFoundException.class, LoanNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BookNotAvailableException.class, DuplicateReservationException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MemberRestrictedException.class)
    public ProblemDetail handleForbidden(MemberRestrictedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({MaxLoansExceededException.class, OverdueLoanException.class})
    public ProblemDetail handleUnprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
