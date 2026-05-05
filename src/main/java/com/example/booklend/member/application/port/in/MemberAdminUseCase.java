package com.example.booklend.member.application.port.in;

import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;

public interface MemberAdminUseCase {

    record AddMemberCommand(String name, String email) {}

    record ClearRestrictionCommand(MemberId memberId) {}

    Member addMember(AddMemberCommand command);

    Member clearRestriction(ClearRestrictionCommand command);
}
