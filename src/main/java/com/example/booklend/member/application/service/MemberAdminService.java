package com.example.booklend.member.application.service;

import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberAdminService implements MemberAdminUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    public MemberAdminService(LoadMemberPort loadMemberPort, SaveMemberPort saveMemberPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
    }

    @Override
    public Member addMember(AddMemberCommand command) {
        Member member = Member.create(MemberId.newId(), command.name(), command.email());
        return saveMemberPort.saveMember(member);
    }

    @Override
    public Member clearRestriction(ClearRestrictionCommand command) {
        Member member = loadMemberPort.loadMember(command.memberId());
        member.clearRestriction();
        return saveMemberPort.saveMember(member);
    }
}
