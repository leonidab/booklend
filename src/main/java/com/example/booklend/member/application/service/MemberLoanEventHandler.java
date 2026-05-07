package com.example.booklend.member.application.service;

import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberLoanEventHandler {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onBookBorrowed(BookBorrowedEvent event) {
        Member member = loadMemberPort.loadMember(new MemberId(event.memberId()));
        member.recordLoanTaken();
        saveMemberPort.saveMember(member);
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onBookReturned(BookReturnedEvent event) {
        Member member = loadMemberPort.loadMember(new MemberId(event.memberId()));
        member.recordLoanReturned(event.wasLate());
        saveMemberPort.saveMember(member);
    }
}
