package com.example.booklend.member.domain;

import com.example.booklend.member.domain.exception.MaxLoansExceededException;
import com.example.booklend.member.domain.exception.MemberRestrictedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    private Member freshMember() {
        return Member.create(MemberId.newId(), "Alice", "alice@test.com");
    }

    @Test
    void newMember_isActive_withZeroLoans() {
        Member member = freshMember();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getActiveLoansCount()).isZero();
    }

    @Test
    void canBorrow_upToThreeLoans() {
        Member member = freshMember();
        member.recordLoanTaken();
        member.recordLoanTaken();
        member.recordLoanTaken();
        assertThat(member.getActiveLoansCount()).isEqualTo(3);
    }

    @Test
    void cannotBorrow_whenThreeLoansTaken() {
        Member member = freshMember();
        member.recordLoanTaken();
        member.recordLoanTaken();
        member.recordLoanTaken();
        assertThatThrownBy(member::assertCanBorrow)
                .isInstanceOf(MaxLoansExceededException.class);
    }

    @Test
    void returningLoan_decrementsCount() {
        Member member = freshMember();
        member.recordLoanTaken();
        member.recordLoanReturned(false, Instant.now());
        assertThat(member.getActiveLoansCount()).isZero();
    }

    @Test
    void moreThanTwoLateReturns_restrictseMember() {
        Member member = freshMember();
        member.recordLoanTaken();
        member.recordLoanReturned(true, Instant.now());
        member.recordLoanTaken();
        member.recordLoanReturned(true, Instant.now());
        member.recordLoanTaken();
        member.recordLoanReturned(true, Instant.now());

        assertThat(member.getStatus()).isEqualTo(MemberStatus.RESTRICTED);
    }

    @Test
    void exactlyTwoLateReturns_doesNotRestrict() {
        Member member = freshMember();
        member.recordLoanTaken();
        member.recordLoanReturned(true, Instant.now());
        member.recordLoanTaken();
        member.recordLoanReturned(true, Instant.now());

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void restrictedMember_cannotBorrow() {
        Member member = freshMember();
        for (int i = 0; i < 3; i++) {
            member.recordLoanTaken();
            member.recordLoanReturned(true, Instant.now());
        }
        assertThatThrownBy(member::assertCanBorrow)
                .isInstanceOf(MemberRestrictedException.class);
    }

    @Test
    void clearRestriction_allowsBorrowingAgain() {
        Member member = freshMember();
        for (int i = 0; i < 3; i++) {
            member.recordLoanTaken();
            member.recordLoanReturned(true, Instant.now());
        }
        member.clearRestriction();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        member.assertCanBorrow();
    }
}
