package com.example.booklend.lending.infrastructure.persistence;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.member.domain.MemberId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "postgres")
public class ReservationPostgresPersistenceAdapter implements LoadReservationPort, SaveReservationPort {

    private final JdbcTemplate jdbc;

    public ReservationPostgresPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Reservation> rowMapper = (rs, rowNum) -> Reservation.reconstitute(
            new ReservationId(rs.getObject("id", UUID.class)),
            new BookId(rs.getObject("book_id", UUID.class)),
            new MemberId(rs.getObject("member_id", UUID.class)),
            rs.getTimestamp("requested_at").toInstant()
    );

    @Override
    public Optional<Reservation> findFirstByBookId(BookId bookId) {
        return jdbc.query(
                "SELECT id, book_id, member_id, requested_at FROM reservations WHERE book_id = ? ORDER BY requested_at ASC LIMIT 1",
                rowMapper, bookId.value()
        ).stream().findFirst();
    }

    @Override
    public Optional<Reservation> findByBookIdAndMemberId(BookId bookId, MemberId memberId) {
        return jdbc.query(
                "SELECT id, book_id, member_id, requested_at FROM reservations WHERE book_id = ? AND member_id = ?",
                rowMapper, bookId.value(), memberId.value()
        ).stream().findFirst();
    }

    @Override
    public List<Reservation> findByMemberId(MemberId memberId) {
        return jdbc.query(
                "SELECT id, book_id, member_id, requested_at FROM reservations WHERE member_id = ?",
                rowMapper, memberId.value()
        );
    }

    @Override
    public Reservation saveReservation(Reservation reservation) {
        jdbc.update("""
                INSERT INTO reservations (id, book_id, member_id, requested_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                reservation.getId().value(),
                reservation.getBookId().value(),
                reservation.getMemberId().value(),
                Timestamp.from(reservation.getRequestedAt())
        );
        return reservation;
    }

    @Override
    public void deleteReservation(ReservationId id) {
        jdbc.update("DELETE FROM reservations WHERE id = ?", id.value());
    }
}
