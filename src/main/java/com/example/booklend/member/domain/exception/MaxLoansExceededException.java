package com.example.booklend.member.domain.exception;

import com.example.booklend.member.domain.MemberId;

public class MaxLoansExceededException extends RuntimeException {
    public MaxLoansExceededException(MemberId memberId, int currentCount) {
        super("Member " + memberId + " already has " + currentCount + " active loans (max 3)");
    }
}
