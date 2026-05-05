package com.example.booklend.lending.infrastructure.persistence;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.lending.infrastructure.persistence.entity.ReservationJpaEntity;
import com.example.booklend.lending.infrastructure.persistence.repository.ReservationJpaRepository;
import com.example.booklend.member.domain.MemberId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "jpa", matchIfMissing = true)
public class ReservationPersistenceAdapter implements LoadReservationPort, SaveReservationPort {

    private final ReservationJpaRepository repository;

    public ReservationPersistenceAdapter(ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Reservation> findFirstByBookId(BookId bookId) {
        return repository.findByBookIdOrderByRequestedAtAsc(bookId.value())
                .stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<Reservation> findByBookIdAndMemberId(BookId bookId, MemberId memberId) {
        return repository.findByBookIdAndMemberId(bookId.value(), memberId.value())
                .map(this::toDomain);
    }

    @Override
    public List<Reservation> findByMemberId(MemberId memberId) {
        return repository.findByMemberId(memberId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public Reservation saveReservation(Reservation reservation) {
        repository.save(toJpa(reservation));
        return reservation;
    }

    @Override
    public void deleteReservation(ReservationId id) {
        repository.deleteById(id.value());
    }

    private Reservation toDomain(ReservationJpaEntity e) {
        return Reservation.reconstitute(
                new ReservationId(e.getId()),
                new BookId(e.getBookId()),
                new MemberId(e.getMemberId()),
                e.getRequestedAt()
        );
    }

    private ReservationJpaEntity toJpa(Reservation r) {
        return new ReservationJpaEntity(
                r.getId().value(),
                r.getBookId().value(),
                r.getMemberId().value(),
                r.getRequestedAt()
        );
    }
}
