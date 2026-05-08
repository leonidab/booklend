package com.example.booklend.lending.application.service;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.lending.application.port.in.ReserveUseCase;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.application.port.out.SaveReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;
import com.example.booklend.lending.domain.exception.DuplicateReservationException;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.shared.application.port.out.ClockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService implements ReserveUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadBookPort loadBookPort;
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final ClockPort clock;

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
