package com.example.booklend.lending.infrastructure.event;

import com.example.booklend.lending.application.port.out.LoadReservationPort;
import com.example.booklend.lending.domain.event.BookReturnedEvent;
import com.example.booklend.member.domain.event.MemberRestrictedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ReservationNotificationHandler.class);

    private final LoadReservationPort loadReservationPort;

    public ReservationNotificationHandler(LoadReservationPort loadReservationPort) {
        this.loadReservationPort = loadReservationPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookReturned(BookReturnedEvent event) {
        loadReservationPort.findFirstByBookId(event.bookId()).ifPresent(reservation ->
                log.info("NOTIFICATION: Book {} is now available — member {} is next in queue",
                        reservation.getBookId(), reservation.getMemberId())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRestricted(MemberRestrictedEvent event) {
        log.warn("Member {} has been RESTRICTED due to repeated late returns", event.memberId());
    }
}
