package com.example.booklend.lending.infrastructure.persistence;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.LoanPeriod;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.exception.LoanNotFoundException;
import com.example.booklend.member.domain.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "postgres")
@RequiredArgsConstructor
public class LoanPostgresPersistenceAdapter implements LoadLoanPort, SaveLoanPort {

    private final JdbcTemplate jdbc;

    private final RowMapper<Loan> rowMapper = (rs, rowNum) -> {
        Timestamp returnedAtTs = rs.getTimestamp("returned_at");
        return Loan.reconstitute(
                new LoanId(rs.getObject("id", UUID.class)),
                new MemberId(rs.getObject("member_id", UUID.class)),
                new BookId(rs.getObject("book_id", UUID.class)),
                new LoanPeriod(
                        rs.getTimestamp("borrowed_at").toInstant(),
                        rs.getTimestamp("due_date").toInstant()
                ),
                returnedAtTs != null ? returnedAtTs.toInstant() : null,
                LoanStatus.valueOf(rs.getString("status"))
        );
    };

    @Override
    public Loan loadLoan(LoanId id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, member_id, book_id, borrowed_at, due_date, returned_at, status FROM loans WHERE id = ?",
                    rowMapper, id.value()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new LoanNotFoundException(id);
        }
    }

    @Override
    public List<Loan> findActiveByMemberId(MemberId memberId) {
        return jdbc.query(
                "SELECT id, member_id, book_id, borrowed_at, due_date, returned_at, status FROM loans WHERE member_id = ? AND status = ?",
                rowMapper, memberId.value(), LoanStatus.ACTIVE.name()
        );
    }

    @Override
    public List<Loan> findAllByMemberId(MemberId memberId) {
        return jdbc.query(
                "SELECT id, member_id, book_id, borrowed_at, due_date, returned_at, status FROM loans WHERE member_id = ?",
                rowMapper, memberId.value()
        );
    }

    @Override
    public void saveLoan(Loan loan) {
        jdbc.update("""
                INSERT INTO loans (id, member_id, book_id, borrowed_at, due_date, returned_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    returned_at = EXCLUDED.returned_at,
                    status = EXCLUDED.status
                """,
                loan.getId().value(),
                loan.getMemberId().value(),
                loan.getBookId().value(),
                Timestamp.from(loan.getPeriod().borrowedAt()),
                Timestamp.from(loan.getPeriod().dueDate()),
                loan.getReturnedAt() != null ? Timestamp.from(loan.getReturnedAt()) : null,
                loan.getStatus().name()
        );
    }
}
