# Architecture

Hexagonal (ports & adapters) + DDD, three bounded contexts in one Spring Boot monolith.

## Package structure

```
com.example.booklend
├── catalog/          Book collection — Book aggregate, availability state
│   ├── api/          REST inbound adapter
│   ├── application/  CatalogAdminUseCase | LoadBookPort, SaveBookPort
│   ├── domain/       Book, BookId, ISBN
│   └── infrastructure/persistence/  JPA + JDBC implementations
│
├── lending/          Borrow/return/reserve lifecycle
│   ├── api/          REST inbound adapter
│   │   └── cli/      BookLendCliRunner — second inbound adapter (ApplicationRunner)
│   ├── application/  BorrowBookUseCase, ReturnBookUseCase, ReserveBookUseCase, LoanQueryUseCase
│   │                 LoadLoanPort, SaveLoanPort, LoadReservationPort, SaveReservationPort
│   ├── domain/       Loan, Reservation aggregates; BookReadyForMemberEvent
│   └── infrastructure/persistence/
│
├── member/           Member identity and borrowing eligibility
│   ├── api/
│   ├── application/  MemberAdminUseCase | LoadMemberPort, SaveMemberPort
│   ├── domain/       Member aggregate, MemberStatus
│   └── infrastructure/persistence/
│
└── shared/           ClockPort, DomainEventPublisher, DomainEvent interface
                      SystemClockAdapter, SpringDomainEventPublisher, outbox persistence
```

`lending` imports `LoadBookPort`/`SaveBookPort` from `catalog` and `LoadMemberPort`/`SaveMemberPort` from `member` — port interfaces only, never JPA classes from another context.

## Aggregates

**Book** (`catalog`) — owns availability state (`markAvailable`/`markUnavailable`, `checkAvailable`). Single authority; lending context goes through `LoadBookPort`/`SaveBookPort`.

**Member** (`member`) — owns borrowing invariants. Denormalises `activeLoansCount` and `lateReturnCount` so `assertCanBorrow` and the max-3/RESTRICTED rules run without querying the loans table.

**Loan** (`lending`) — owns the borrow/return lifecycle. `LoanPeriod` encapsulates the 14-day rule and overdue logic. Carries a domain event list; the service registers `BookReadyForMemberEvent` after business writes, then pulls and publishes.

**Reservation** (`lending`) — queue position and access-control token. Kept separate from `Book` so the Book aggregate never loads an unbounded reservation list.

## Ports and why each earns its place

| Port                                          | Why                                                                                       |
|-----------------------------------------------|-------------------------------------------------------------------------------------------|
| `LoadBookPort` / `SaveBookPort`               | Lending needs Book state without depending on catalog JPA; catalog stays the sole mutator |
| `LoadMemberPort` / `SaveMemberPort`           | Same isolation for member context                                                         |
| `LoadLoanPort` / `SaveLoanPort`               | Persistence technology swappable (JPA ↔ JDBC) without touching services                   |
| `LoadReservationPort` / `SaveReservationPort` | Reservation lifecycle isolated; enables queue-enforcement logic in service                |
| `ClockPort`                                   | Makes time injectable — `FakeClockAdapter` in tests without mocking                       |
| `DomainEventPublisher`                        | Decouples domain from outbox implementation; replaced in unit tests                       |

Persistence adapters have two implementations each (`booklend.persistence=jpa|postgres`), selected via `@ConditionalOnProperty`. Swap requires only a property change.

## Trade-offs

- **Reservation held after return** — not deleted on return; serves as both queue position and exclusive-borrow access control. Cleared atomically when the reserved member borrows. Avoids an extra "on-hold" state on Book.
- **`BookReadyForMemberEvent` not `BookReturnedEvent`** — the outbox carries the actionable event (who to notify). A generic returned event had no downstream consumer and was removed.
- **Outbox via `@TransactionalEventListener(BEFORE_COMMIT)`** — event and all business writes commit atomically. Downstream delivery is at-least-once (Debezium → Kafka); consumers must be idempotent.
- **Denormalised loan count** — `Member.activeLoansCount` maintained by lending services, not computed by query. Fast aggregate autonomy at the cost of count drift if a service bug skips the update.
- **Cross-context port sharing** — `lending` imports ports from `catalog` and `member` rather than defining lending-specific projections. Pragmatic for a monolith; couples lending to their port contracts.
- **`@DomainEvents` rejected** — Spring Data's `@DomainEvents` requires saving the domain object directly, conflicting with the persistence model separation. Manual `registerEvent`/`pullDomainEvents` keeps domain classes Spring-free.
