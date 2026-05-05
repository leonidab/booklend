package com.example.booklend.member.infrastructure.persistence;

import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.application.port.out.SaveMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.MemberStatus;
import com.example.booklend.member.domain.exception.MemberNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "postgres")
public class MemberPostgresPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

    private final JdbcTemplate jdbc;

    public MemberPostgresPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Member> rowMapper = (rs, rowNum) -> Member.reconstitute(
            new MemberId(rs.getObject("id", UUID.class)),
            rs.getString("name"),
            rs.getString("email"),
            MemberStatus.valueOf(rs.getString("status")),
            rs.getInt("active_loans_count"),
            rs.getInt("late_return_count")
    );

    @Override
    public Member loadMember(MemberId id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, name, email, status, active_loans_count, late_return_count FROM members WHERE id = ?",
                    rowMapper, id.value()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new MemberNotFoundException(id);
        }
    }

    @Override
    public Member saveMember(Member member) {
        jdbc.update("""
                INSERT INTO members (id, name, email, status, active_loans_count, late_return_count)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    active_loans_count = EXCLUDED.active_loans_count,
                    late_return_count = EXCLUDED.late_return_count
                """,
                member.getId().value(),
                member.getName(),
                member.getEmail(),
                member.getStatus().name(),
                member.getActiveLoansCount(),
                member.getLateReturnCount()
        );
        return member;
    }
}
