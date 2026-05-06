package com.example.booklend.member.domain;

import com.example.booklend.member.domain.exception.MaxLoansExceededException;
import com.example.booklend.member.domain.exception.MemberRestrictedException;
import lombok.Getter;

@Getter
public class Member {

    private static final int MAX_ACTIVE_LOANS = 3;
    private static final int LATE_RETURNS_THRESHOLD = 2;

    private final MemberId id;
    private final String name;
    private final String email;
    private MemberStatus status;
    private int activeLoansCount;
    private int lateReturnCount;

    private Member(MemberId id, String name, String email,
                   MemberStatus status, int activeLoansCount, int lateReturnCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.activeLoansCount = activeLoansCount;
        this.lateReturnCount = lateReturnCount;
    }

    public static Member create(MemberId id, String name, String email) {
        return new Member(id, name, email, MemberStatus.ACTIVE, 0, 0);
    }

    public static Member reconstitute(MemberId id, String name, String email,
                                      MemberStatus status, int activeLoansCount, int lateReturnCount) {
        return new Member(id, name, email, status, activeLoansCount, lateReturnCount);
    }

    public void assertCanBorrow() {
        if (status == MemberStatus.RESTRICTED) {
            throw new MemberRestrictedException(id);
        }
        if (activeLoansCount >= MAX_ACTIVE_LOANS) {
            throw new MaxLoansExceededException(id, activeLoansCount);
        }
    }

    public void recordLoanTaken() {
        assertCanBorrow();
        activeLoansCount++;
    }

    public void recordLoanReturned(boolean wasLate) {
        if (activeLoansCount <= 0) {
            throw new IllegalStateException("No active loans to return for member " + id);
        }
        activeLoansCount--;
        if (wasLate) {
            lateReturnCount++;
            if (lateReturnCount > LATE_RETURNS_THRESHOLD) {
                status = MemberStatus.RESTRICTED;
            }
        }
    }

    public void clearRestriction() {
        this.status = MemberStatus.ACTIVE;
    }
}
