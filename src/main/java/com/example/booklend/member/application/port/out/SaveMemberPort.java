package com.example.booklend.member.application.port.out;

import com.example.booklend.member.domain.Member;

public interface SaveMemberPort {
    Member saveMember(Member member);
}
