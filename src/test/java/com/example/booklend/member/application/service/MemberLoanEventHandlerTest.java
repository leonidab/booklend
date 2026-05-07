package com.example.booklend.member.application.service;

import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import com.example.booklend.member.domain.MemberStatus;
import com.example.booklend.member.domain.exception.MaxLoansExceededException;
import com.example.booklend.member.domain.exception.MemberNotFoundException;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberLoanEventHandlerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryMemberRepository memberRepo;
    private MemberLoanEventHandler handler;

    @BeforeEach
    void setUp() {
        memberRepo = new InMemoryMemberRepository();
        handler = new MemberLoanEventHandler(memberRepo, memberRepo);
    }

    private Member savedMember() {
        Member m = Member.create(MemberId.newId(), "Alice", "alice@test.com");
        memberRepo.save(m);
        return m;
    }

    @Test
    void onBookBorrowed_incrementsActiveLoansCount() {
        Member member = savedMember();

        handler.onBookBorrowed(new BookBorrowedEvent(
                UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), NOW));

        assertThat(memberRepo.loadMember(member.getId()).getActiveLoansCount()).isEqualTo(1);
    }

    @Test
    void onBookBorrowed_throwsMaxLoansExceeded_atFourthLoan() {
        Member member = savedMember();
        for (int i = 0; i < 3; i++) {
            handler.onBookBorrowed(new BookBorrowedEvent(
                    UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), NOW));
        }

        assertThatThrownBy(() -> handler.onBookBorrowed(new BookBorrowedEvent(
                UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), NOW)))
                .isInstanceOf(MaxLoansExceededException.class);
    }

    @Test
    void onBookBorrowed_throwsMemberNotFound_whenMemberMissing() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> handler.onBookBorrowed(new BookBorrowedEvent(
                UUID.randomUUID(), unknown, UUID.randomUUID(), NOW)))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void onBookReturned_decrementsActiveLoansCount() {
        Member member = savedMember();
        member.recordLoanTaken();
        memberRepo.save(member);

        handler.onBookReturned(new BookReturnedEvent(
                UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), false, NOW));

        assertThat(memberRepo.loadMember(member.getId()).getActiveLoansCount()).isZero();
    }

    @Test
    void onBookReturned_lateReturn_incrementsLateCount() {
        Member member = savedMember();
        member.recordLoanTaken();
        memberRepo.save(member);

        handler.onBookReturned(new BookReturnedEvent(
                UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), true, NOW));

        assertThat(memberRepo.loadMember(member.getId()).getLateReturnCount()).isEqualTo(1);
    }

    @Test
    void onBookReturned_thirdLateReturn_restrictsMember() {
        Member member = savedMember();
        for (int i = 0; i < 3; i++) {
            handler.onBookBorrowed(new BookBorrowedEvent(
                    UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), NOW));
            handler.onBookReturned(new BookReturnedEvent(
                    UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), true, NOW));
        }

        assertThat(memberRepo.loadMember(member.getId()).getStatus()).isEqualTo(MemberStatus.RESTRICTED);
    }

    @Test
    void onBookReturned_throwsMemberNotFound_whenMemberMissing() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> handler.onBookReturned(new BookReturnedEvent(
                UUID.randomUUID(), unknown, UUID.randomUUID(), false, NOW)))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void onBookReturned_throwsIllegalState_whenNoActiveLoans() {
        Member member = savedMember();

        assertThatThrownBy(() -> handler.onBookReturned(new BookReturnedEvent(
                UUID.randomUUID(), member.getId().value(), UUID.randomUUID(), false, NOW)))
                .isInstanceOf(IllegalStateException.class);
    }

}
