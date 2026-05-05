package com.example.booklend.member.application.port.out;

import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;

public interface LoadMemberPort {
    Member loadMember(MemberId id);
}
