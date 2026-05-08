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
                      AggregateRoot base class, BookBorrowedEvent, BookReturnedEvent
                      SystemClockAdapter, SpringDomainEventPublisher, outbox persistence
```

`lending` owns Loan/Reservation only. Cross-context state changes (Member counters, Book availability) flow as domain events: lending publishes `BookBorrowedEvent` / `BookReturnedEvent` (in `shared.domain.event`, UUID payload only); `MemberLoanEventHandler` and `BookLoanEventHandler` react in their own context. Lending no longer mutates foreign aggregates.

## Aggregates

**Book** (`catalog`) — owns availability invariant. `loanedOut()` rejects if not available; `returned()` rejects if already available. Mutated only by catalog's own event handler.

**Member** (`member`) — owns borrowing invariants. Denormalises `activeLoansCount` and `lateReturnCount` so `assertCanBorrow` and the max-3/RESTRICTED rules run without querying the loans table. Mutated only by member's own event handler.

**Loan** (`lending`) — owns the borrow/return lifecycle. `LoanPeriod` encapsulates the 14-day rule and overdue logic. Emits `BookBorrowedEvent` from `Loan.create` and `BookReturnedEvent` from `Loan.returnLoan`; service registers `BookReadyForMemberEvent` if a queue exists, then pulls and publishes. Static helper `Loan.assertNoOverdue(loans, now, memberId)` enforces the cross-loan precondition (rule + exception both live on the aggregate type). Extends `shared.domain.AggregateRoot` for event collection.

**Reservation** (`lending`) — queue position and access-control token. `assertClaimableBy(memberId)` enforces the access rule inside the aggregate. Kept separate from `Book` so the Book aggregate never loads an unbounded reservation list.

**`AggregateRoot`** (`shared.domain`) — abstract base providing `registerEvent` / `pullDomainEvents`. Currently extended only by `Loan` (the only aggregate that emits events; Member, Book, Reservation react). Available to any future emitting aggregate.

## Ports and why each earns its place

| Port                                          | Why                                                                                                  |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------|
| `LoadBookPort` / `SaveBookPort`               | Used only inside `catalog` (admin + event handler). Other contexts never touch books directly.       |
| `LoadMemberPort` / `SaveMemberPort`           | Used only inside `member` (admin + event handler). Other contexts never touch members directly.      |
| `LoadLoanPort` / `SaveLoanPort`               | Persistence technology swappable (JPA ↔ JDBC) without touching services                              |
| `LoadReservationPort` / `SaveReservationPort` | Reservation lifecycle isolated; queue-enforcement rule lives on the aggregate (`assertClaimableBy`)  |
| `ClockPort`                                   | Makes time injectable — `FakeClockAdapter` in tests without mocking                                  |
| `DomainEventPublisher`                        | Decouples domain from outbox implementation; in-memory variant dispatches to registered test handlers |

Persistence adapters have two implementations each (`booklend.persistence=jpa|postgres`), selected via `@ConditionalOnProperty`. Swap requires only a property change.

## Trade-offs

- **Reservation held after return** — not deleted on return; serves as both queue position and exclusive-borrow access control. Cleared atomically when the reserved member borrows. Avoids an extra "on-hold" state on Book.
- **Cross-aggregate via in-process domain events (NOT eventual consistency)** — lending publishes `BookBorrowedEvent` / `BookReturnedEvent`; catalog and member listeners (`@EventListener`, `Propagation.MANDATORY`) run synchronously in the same call stack and same transaction. Listener exception → tx rollback → loan never commits. Strong consistency, identical atomicity to direct mutation. The pattern is for **code organization** (each context owns its own state; lending doesn't reach into Member/Book) and **migration optionality** (swap to `@TransactionalEventListener(AFTER_COMMIT)` + outbox + broker → real eventual consistency, no domain changes). The outbox pipeline is the actually-eventual piece.
- **Events use UUIDs not typed IDs** — `shared.domain.event.*` records carry raw `UUID` so the events package doesn't pull `BookId`/`MemberId` from sibling contexts (would create ArchUnit-detected slice cycles). Consumers wrap into typed IDs when they load aggregates.
- **`BookReadyForMemberEvent` kept in `lending.domain.event`** — only consumed by the outbox via the `DomainEvent` super-interface, so no cross-context import. Carries typed IDs for richer downstream payload.
- **Outbox via `@TransactionalEventListener(BEFORE_COMMIT)`** — event and all business writes commit atomically. Downstream delivery is at-least-once; consumers must be idempotent.
- **Denormalised loan count** — `Member.activeLoansCount` maintained by `MemberLoanEventHandler`, not computed by query. Fast aggregate autonomy at the cost of count drift if an event is lost (would not happen in same-tx in-process delivery, would matter if listeners moved out-of-process).
- **`@DomainEvents` rejected** — Spring Data's `@DomainEvents` requires saving the domain object directly, conflicting with the persistence model separation. Manual `registerEvent`/`pullDomainEvents` keeps domain classes Spring-free.

## Tests

Three layers:

| Layer               | Examples                                                                          | Spring?           | DB                                                                      |
|---------------------|-----------------------------------------------------------------------------------|-------------------|-------------------------------------------------------------------------|
| Domain unit         | `LoanTest`, `MemberTest`                                                          | No                | None                                                                    |
| Application unit    | `BorrowBookServiceTest`, `MemberLoanEventHandlerTest`, `BookLoanEventHandlerTest` | No                | `InMemory*Repository` fakes implementing the same ports as JPA adapters |
| Integration (`*IT`) | `BookReturnedEventIT`, `LoanControllerIT`, `BookPersistenceAdapterIT`             | `@SpringBootTest` | H2 via `it` profile (separate JDBC URL from dev)                        |

**Profile separation**: dev profile runs `data.sql` seed for manual exploration. IT tests use `@ActiveProfiles("it")` → `application-it.properties` → distinct H2 instance, no seed (`spring.sql.init.mode=never`). Tests build their own fixtures via `addBook()` / `addMember()` helpers. Schema generated from JPA entities by Hibernate `ddl-auto=create-drop` in both profiles → guaranteed parity. No Docker required.

**Trade-off**: H2 is the production DB for this project, so H2-in-IT is realistic. If prod ever moves to Postgres, swap IT profile to Testcontainers Postgres; entity-derived schema still works, but real schema management would need migrations (Flyway / Liquibase) to keep dev / IT / prod aligned.

**ArchUnit** (`HexagonalArchitectureTest`) enforces: domain has no Spring/JPA deps, domain doesn't depend on application or infrastructure, application doesn't depend on infrastructure, inbound adapters don't reach past the use case ports, bounded contexts are cycle-free.
