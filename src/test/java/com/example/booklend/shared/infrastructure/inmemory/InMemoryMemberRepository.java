package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.exception.MemberNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMemberRepository implements LoadMemberPort, SaveMemberPort {

    private final Map<MemberId, Member> store = new ConcurrentHashMap<>();

    public void save(Member member) { store.put(member.getId(), member); }

    @Override
    public Member loadMember(MemberId id) {
        Member member = store.get(id);
        if (member == null) throw new MemberNotFoundException(id);
        return member;
    }

    @Override
    public Member saveMember(Member member) { store.put(member.getId(), member); return member; }
}
