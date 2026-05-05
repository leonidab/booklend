package com.example.booklend.member.infrastructure.persistence;

import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.MemberStatus;
import com.example.booklend.member.domain.exception.MemberNotFoundException;
import com.example.booklend.member.infrastructure.persistence.entity.MemberJpaEntity;
import com.example.booklend.member.infrastructure.persistence.repository.MemberJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "jpa", matchIfMissing = true)
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

    private final MemberJpaRepository repository;

    public MemberPersistenceAdapter(MemberJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Member loadMember(MemberId id) {
        return repository.findById(id.value())
                .map(this::toDomain)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    @Override
    public Member saveMember(Member member) {
        MemberJpaEntity existing = repository.findById(member.getId().value()).orElse(null);
        if (existing != null) {
            existing.setStatus(member.getStatus().name());
            existing.setActiveLoansCount(member.getActiveLoansCount());
            existing.setLateReturnCount(member.getLateReturnCount());
            repository.save(existing);
        } else {
            repository.save(toJpa(member));
        }
        return member;
    }

    private Member toDomain(MemberJpaEntity e) {
        return Member.reconstitute(
                new MemberId(e.getId()),
                e.getName(),
                e.getEmail(),
                MemberStatus.valueOf(e.getStatus()),
                e.getActiveLoansCount(),
                e.getLateReturnCount()
        );
    }

    private MemberJpaEntity toJpa(Member m) {
        return new MemberJpaEntity(
                m.getId().value(),
                m.getName(),
                m.getEmail(),
                m.getStatus().name(),
                m.getActiveLoansCount(),
                m.getLateReturnCount()
        );
    }
}
