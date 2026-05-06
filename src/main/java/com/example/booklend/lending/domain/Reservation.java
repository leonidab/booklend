package com.example.booklend.lending.domain;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.member.domain.MemberId;
import lombok.Getter;

import java.time.Instant;

@Getter
public class Reservation {

    private final ReservationId id;
    private final BookId bookId;
    private final MemberId memberId;
    private final Instant requestedAt;

    private Reservation(ReservationId id, BookId bookId, MemberId memberId, Instant requestedAt) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.requestedAt = requestedAt;
    }

    public static Reservation create(ReservationId id, BookId bookId, MemberId memberId, Instant requestedAt) {
        return new Reservation(id, bookId, memberId, requestedAt);
    }

    public static Reservation reconstitute(ReservationId id, BookId bookId, MemberId memberId, Instant requestedAt) {
        return new Reservation(id, bookId, memberId, requestedAt);
    }
}
