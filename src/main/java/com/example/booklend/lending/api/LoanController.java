package com.example.booklend.lending.api;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.api.dto.BorrowRequest;
import com.example.booklend.lending.application.port.in.BorrowUseCase;
import com.example.booklend.lending.application.port.in.LoanQueryUseCase;
import com.example.booklend.lending.application.port.in.ReturnUseCase;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.member.domain.MemberId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final BorrowUseCase borrowUseCase;
    private final ReturnUseCase returnUseCase;
    private final LoanQueryUseCase loanQueryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LoanResponse borrow(@RequestBody BorrowRequest request) {
        Loan loan = borrowUseCase.borrow(new BorrowUseCase.BorrowCommand(
                MemberId.of(request.memberId()),
                BookId.of(request.bookId())
        ));
        return LoanResponse.from(loan);
    }

    @PostMapping("/{loanId}/return")
    LoanResponse returnBook(@PathVariable String loanId) {
        Loan loan = returnUseCase.returnBook(
                new ReturnUseCase.ReturnCommand(LoanId.of(loanId)));
        return LoanResponse.from(loan);
    }

    @GetMapping
    List<LoanResponse> getLoans(@RequestParam String memberId,
                                       @RequestParam(defaultValue = "false") boolean all) {
        MemberId id = MemberId.of(memberId);
        List<Loan> loans = all
                ? loanQueryUseCase.findAllLoans(id)
                : loanQueryUseCase.findActiveLoans(id);
        return loans.stream().map(LoanResponse::from).toList();
    }

    record LoanResponse(String id, String memberId, String bookId,
                        Instant borrowedAt, Instant dueDate, Instant returnedAt, String status) {
        static LoanResponse from(Loan loan) {
            return new LoanResponse(
                    loan.getId().toString(),
                    loan.getMemberId().toString(),
                    loan.getBookId().toString(),
                    loan.getPeriod().borrowedAt(),
                    loan.getPeriod().dueDate(),
                    loan.getReturnedAt(),
                    loan.getStatus().name()
            );
        }
    }
}
