package com.example.booklend.member.domain.exception;

import com.example.booklend.member.domain.MemberId;

public class MemberRestrictedException extends RuntimeException {
    public MemberRestrictedException(MemberId memberId) {
        super("Member " + memberId + " is RESTRICTED and cannot borrow books");
    }
}
