package com.example.booklend.lending.api;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.api.dto.ReserveRequest;
import com.example.booklend.lending.application.port.in.ReserveBookUseCase;
import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.member.domain.MemberId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReserveBookUseCase reserveBookUseCase;
    private final LoadReservationPort loadReservationPort;

    public ReservationController(ReserveBookUseCase reserveBookUseCase,
                                 LoadReservationPort loadReservationPort) {
        this.reserveBookUseCase = reserveBookUseCase;
        this.loadReservationPort = loadReservationPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse reserve(@RequestBody ReserveRequest request) {
        Reservation reservation = reserveBookUseCase.reserve(new ReserveBookUseCase.ReserveCommand(
                MemberId.of(request.memberId()),
                BookId.of(request.bookId())
        ));
        return ReservationResponse.from(reservation);
    }

    @GetMapping
    List<ReservationResponse> getReservations(@RequestParam String memberId) {
        return loadReservationPort.findByMemberId(MemberId.of(memberId))
                .stream().map(ReservationResponse::from).toList();
    }

    record ReservationResponse(String id, String bookId, String memberId, Instant requestedAt) {
        static ReservationResponse from(Reservation r) {
            return new ReservationResponse(
                    r.getId().toString(),
                    r.getBookId().toString(),
                    r.getMemberId().toString(),
                    r.getRequestedAt()
            );
        }
    }
}
