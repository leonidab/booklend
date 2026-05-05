package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.lending.application.port.in.ReserveBookUseCase;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.lending.domain.exception.DuplicateReservationException;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.shared.application.port.out.ClockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReserveBookService implements ReserveBookUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadBookPort loadBookPort;
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final ClockPort clock;

    public ReserveBookService(LoadMemberPort loadMemberPort, LoadBookPort loadBookPort,
                              LoadReservationPort loadReservationPort,
                              SaveReservationPort saveReservationPort, ClockPort clock) {
        this.loadMemberPort = loadMemberPort;
        this.loadBookPort = loadBookPort;
        this.loadReservationPort = loadReservationPort;
        this.saveReservationPort = saveReservationPort;
        this.clock = clock;
    }

    @Override
    public Reservation reserve(ReserveCommand command) {
        loadMemberPort.loadMember(command.memberId());
        loadBookPort.loadBook(command.bookId());

        loadReservationPort.findByBookIdAndMemberId(command.bookId(), command.memberId())
                .ifPresent(existing -> {
                    throw new DuplicateReservationException(command.bookId(), command.memberId());
                });

        Reservation reservation = Reservation.create(
                ReservationId.newId(),
                command.bookId(),
                command.memberId(),
                clock.now()
        );

        return saveReservationPort.saveReservation(reservation);
    }
}
