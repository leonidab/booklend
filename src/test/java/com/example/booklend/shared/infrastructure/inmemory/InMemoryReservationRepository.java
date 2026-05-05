package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.member.domain.MemberId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryReservationRepository implements LoadReservationPort, SaveReservationPort {

    private final Map<ReservationId, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Reservation> findFirstByBookId(BookId bookId) {
        return store.values().stream()
                .filter(r -> r.getBookId().equals(bookId))
                .min(Comparator.comparing(Reservation::getRequestedAt));
    }

    @Override
    public Optional<Reservation> findByBookIdAndMemberId(BookId bookId, MemberId memberId) {
        return store.values().stream()
                .filter(r -> r.getBookId().equals(bookId) && r.getMemberId().equals(memberId))
                .findFirst();
    }

    @Override
    public List<Reservation> findByMemberId(MemberId memberId) {
        return store.values().stream()
                .filter(r -> r.getMemberId().equals(memberId))
                .toList();
    }

    @Override
    public Reservation saveReservation(Reservation reservation) {
        store.put(reservation.getId(), reservation);
        return reservation;
    }

    @Override
    public void deleteReservation(ReservationId id) {
        store.remove(id);
    }
}
