package com.example.booklend.member.domain.exception;

import com.example.booklend.member.domain.MemberId;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(MemberId memberId) {
        super("Member not found: " + memberId);
    }
}
