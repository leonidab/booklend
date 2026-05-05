package com.example.booklend.lending.application.port.out;

import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.lending.domain.ReservationId;

public interface SaveReservationPort {
    Reservation saveReservation(Reservation reservation);
    void deleteReservation(ReservationId id);
}
