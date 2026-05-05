package com.example.booklend.lending.domain.exception;

import com.example.booklend.member.domain.MemberId;

public class OverdueLoanException extends RuntimeException {
    public OverdueLoanException(MemberId memberId) {
        super("Member " + memberId + " has overdue loans and cannot borrow until they are returned");
    }
}
