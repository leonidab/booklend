package com.example.booklend.lending.infrastructure.persistence;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.lending.application.port.out.LoadLoanPort;
import com.example.booklend.lending.application.port.out.SaveLoanPort;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.LoanPeriod;
import com.example.booklend.lending.domain.LoanStatus;
import com.example.booklend.lending.domain.exception.LoanNotFoundException;
import com.example.booklend.lending.infrastructure.persistence.entity.LoanJpaEntity;
import com.example.booklend.lending.infrastructure.persistence.repository.LoanJpaRepository;
import com.example.booklend.member.domain.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class LoanPersistenceAdapter implements LoadLoanPort, SaveLoanPort {

    private final LoanJpaRepository repository;

    @Override
    public Loan loadLoan(LoanId id) {
        return repository.findById(id.value())
                .map(this::toDomain)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    @Override
    public List<Loan> findActiveByMemberId(MemberId memberId) {
        return repository.findByMemberIdAndStatus(memberId.value(), LoanStatus.ACTIVE.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Loan> findAllByMemberId(MemberId memberId) {
        return repository.findByMemberId(memberId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void saveLoan(Loan loan) {
        LoanJpaEntity existing = repository.findById(loan.getId().value()).orElse(null);
        if (existing != null) {
            existing.setReturnedAt(loan.getReturnedAt());
            existing.setStatus(loan.getStatus().name());
            repository.save(existing);
        } else {
            repository.save(toJpa(loan));
        }
    }

    private Loan toDomain(LoanJpaEntity e) {
        return Loan.reconstitute(
                new LoanId(e.getId()),
                new MemberId(e.getMemberId()),
                new BookId(e.getBookId()),
                new LoanPeriod(e.getBorrowedAt(), e.getDueDate()),
                e.getReturnedAt(),
                LoanStatus.valueOf(e.getStatus())
        );
    }

    private LoanJpaEntity toJpa(Loan l) {
        return new LoanJpaEntity(
                l.getId().value(),
                l.getMemberId().value(),
                l.getBookId().value(),
                l.getPeriod().borrowedAt(),
                l.getPeriod().dueDate(),
                l.getReturnedAt(),
                l.getStatus().name()
        );
    }
}
