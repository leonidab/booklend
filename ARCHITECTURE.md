# Architecture

## Design style

Bounded-context-first DDD with hexagonal (ports & adapters) architecture per context. Package layout uses Spring Clean Architecture naming: `api` / `application` / `domain` / `infrastructure`.

## Package structure

```
com.example.booklend
├── catalog/                      Bounded context: book collection management
│   ├── api/                      REST controllers, DTOs (inbound adapter)
│   ├── application/
│   │   ├── port/in/              CatalogAdminUseCase
│   │   └── port/out/             LoadBookPort, SaveBookPort
│   ├── domain/                   Book (aggregate root), BookId, ISBN
│   │   └── exception/            BookNotFoundException, BookNotAvailableException
│   └── infrastructure/
│       └── persistence/          BookPersistenceAdapter (JPA, @ConditionalOnProperty jpa)
│                                 BookPostgresPersistenceAdapter (JDBC, @ConditionalOnProperty postgres)
│                                 BookJpaEntity, BookJpaRepository
│
├── lending/                      Bounded context: borrow/return/reserve lifecycle
│   ├── api/                      REST controllers, DTOs (inbound adapter)
│   ├── application/
│   │   ├── port/in/              BorrowBookUseCase, ReturnBookUseCase,
│   │   │                         ReserveBookUseCase, LoanQueryUseCase
│   │   └── port/out/             LoadLoanPort, SaveLoanPort,
│   │                             LoadReservationPort, SaveReservationPort
│   ├── domain/                   Loan (aggregate root), LoanId, LoanPeriod, LoanStatus
│   │                             Reservation (aggregate root), ReservationId
│   │   ├── event/                BookReturnedEvent
│   │   └── exception/            LoanNotFoundException, DuplicateReservationException,
│   │                             OverdueLoanException
│   └── infrastructure/
│       ├── persistence/          LoanPersistenceAdapter, ReservationPersistenceAdapter (JPA)
│       │                         LoanPostgresPersistenceAdapter, ReservationPostgresPersistenceAdapter (JDBC)
│       │                         JPA entities and Spring Data repositories
│       └── event/                ReservationNotificationHandler (@TransactionalEventListener)
│
├── member/                       Bounded context: member identity and borrowing eligibility
│   ├── api/                      REST controllers, DTOs (inbound adapter)
│   ├── application/
│   │   ├── port/in/              MemberAdminUseCase
│   │   └── port/out/             LoadMemberPort, SaveMemberPort
│   ├── domain/                   Member (aggregate root), MemberId, MemberStatus
│   │   ├── event/                MemberRestrictedEvent
│   │   └── exception/            MemberNotFoundException, MemberRestrictedException,
│   │                             MaxLoansExceededException
│   └── infrastructure/
│       └── persistence/          MemberPersistenceAdapter (JPA), MemberPostgresPersistenceAdapter (JDBC)
│                                 MemberJpaEntity, MemberJpaRepository
│
├── cli/                          BookLendCliRunner — second inbound adapter (ApplicationRunner)
│                                 Drives the same use case ports as the REST controllers
│
├── shared/                       Cross-cutting infrastructure and contracts
│   ├── application/
│   │   └── port/out/             ClockPort, DomainEventPublisher
│   ├── domain/
│   │   └── event/                DomainEvent (interface)
│   └── infrastructure/
│       ├── clock/                SystemClockAdapter
│       └── event/                SpringDomainEventPublisher
│
└── web/                          GlobalExceptionHandler (@RestControllerAdvice)
                                  Top-level — imports from all contexts, excluded from slice cycle rules
```

## Bounded contexts

### catalog
Owns the physical book collection. Sole authority over book availability (`markAvailable` / `markUnavailable`). Nothing outside this context mutates `Book` state directly — lending services receive `Book` via `LoadBookPort` and persist changes via `SaveBookPort`, both defined in `catalog.application.port.out`.

### lending
Owns the borrow/return/reserve lifecycle. `Loan` and `Reservation` are its aggregate roots. Coordinates cross-context reads via ports from `catalog` (`LoadBookPort`, `SaveBookPort`) and `member` (`LoadMemberPort`, `SaveMemberPort`). All writes to other contexts happen within one `@Transactional` boundary — the monolith DB keeps ACID guarantees.

### member
Owns member identity and borrowing eligibility. `Member` tracks `activeLoansCount` and `lateReturnCount` as denormalised counters to enforce borrowing invariants (`assertCanBorrow`, max-3 loans, RESTRICTED status) without querying the loans table. Updates are driven by `lending` application services.

### shared
Cross-cutting contracts that no single context owns: `DomainEvent` interface, `ClockPort`, `DomainEventPublisher`, `GlobalExceptionHandler`. `BookLendCliRunner` lives at the top-level `cli` package — it is an inbound adapter, not shared infrastructure.

## Cross-context dependency rules

| From                  | To                                        | What is shared                     | Allowed?                                                          |
|-----------------------|-------------------------------------------|------------------------------------|-------------------------------------------------------------------|
| `lending.domain`      | `catalog.domain`                          | `BookId` (identity only)           | Yes — ID reference, no behaviour                                  |
| `lending.domain`      | `member.domain`                           | `MemberId` (identity only)         | Yes — ID reference, no behaviour                                  |
| `lending.application` | `catalog.application.port.out`            | `LoadBookPort`, `SaveBookPort`     | Yes — port interface, not implementation                          |
| `lending.application` | `member.application.port.out`             | `LoadMemberPort`, `SaveMemberPort` | Yes — port interface, not implementation                          |
| Any context           | Any other context's domain model (not ID) | full aggregate objects             | Acceptable in a monolith; use ports for cross-context data access |
| `shared`              | Any context                               | nothing                            | Shared has no context dependencies                                |

Identity types (`BookId`, `MemberId`) are small value objects that travel freely across context boundaries. Full aggregate objects (`Book`, `Member`) cross context lines only via port interfaces — the lending context never accesses JPA or persistence classes from another context directly.

## Aggregates

**Book** (`catalog`) — root for availability state. Enforces `checkAvailable()`. Does not know about reservations. Availability is a single boolean appropriate for a single-copy lending model.

**Member** (`member`) — root for borrowing invariants. Tracks `activeLoansCount` (denormalised) and `lateReturnCount`. Invariants (`assertCanBorrow`, max-3, RESTRICTED flag) are enforced in the aggregate without loading loans. Trade-off: count is maintained by `lending` services on every borrow/return — requires discipline but avoids expensive queries inside the aggregate boundary.

**Loan** (`lending`) — root for the borrow/return lifecycle. Contains `LoanPeriod` (14-day rule, overdue/late logic). `returnLoan` is enforced on the aggregate and registers `BookReturnedEvent` internally. Services supply current time via `ClockPort`.

**Reservation** (`lending`) — lightweight aggregate, no state transitions beyond creation and deletion. Kept separate from `Book`: the Book aggregate does not need to load an unbounded list of reservations to enforce its own invariants.

## Use cases and ports

| Use case              | Context | Two adapters?                                       |
|-----------------------|---------|-----------------------------------------------------|
| `BorrowBookUseCase`   | lending | `LoanController` (REST) + `BookLendCliRunner` (CLI) |
| `ReturnBookUseCase`   | lending | `LoanController` + `BookLendCliRunner`              |
| `ReserveBookUseCase`  | lending | `ReservationController` + `BookLendCliRunner`       |
| `LoanQueryUseCase`    | lending | `LoanController` + `BookLendCliRunner`              |
| `CatalogAdminUseCase` | catalog | `BookController` + `BookLendCliRunner`              |
| `MemberAdminUseCase`  | member  | `MemberController` + `BookLendCliRunner`            |

`ClockPort` makes time injectable — `SystemClockAdapter` in production, `FakeClockAdapter` in unit tests. `DomainEventPublisher` decouples services from Spring's `ApplicationEventPublisher` — replaced by `InMemoryDomainEventPublisher` in unit tests.

## Persistence adapters

Each persistence port has two implementations selected via `@ConditionalOnProperty(name = "booklend.persistence")`:

| Value | Technology | Profile |
|---|---|---|
| `jpa` (default) | Spring Data JPA + Hibernate | `application-dev.properties` |
| `postgres` | `JdbcTemplate` + explicit SQL + `ON CONFLICT` upserts | `application-prod.properties` |

Swapping persistence technology requires zero changes to domain or application layer — only the property value changes.

## Event flows

### Flow 1: Book returned → reservation notification

Domain aggregates register their own events. `ReturnBookService` collects and publishes them after all state is saved.

```
ReturnBookService (lending.application)
  │
  ├─ loan.returnLoan(now)
  │     → Loan registers BookReturnedEvent internally
  │
  ├─ member.recordLoanReturned(wasLate, now)
  │     → Member registers MemberRestrictedEvent internally (if threshold exceeded)
  │
  ├─ book.markAvailable()
  │
  ├─ saveLoan / saveMember / saveBook          ← all in one @Transactional
  │
  ├─ loadReservationPort.findFirstByBookId()
  │     if present: saveReservationPort.deleteReservation()
  │     (reservation delete is domain logic — lives in the transaction, not in a listener)
  │
  └─ loan.pullDomainEvents()  → eventPublisher.publish(BookReturnedEvent)
     member.pullDomainEvents() → eventPublisher.publish(MemberRestrictedEvent) [if triggered]
          │
          └─▶ ReservationNotificationHandler
                  @TransactionalEventListener(AFTER_COMMIT)
                  Fires after commit — notification failure cannot roll back the return
                  └─ log.info("NOTIFICATION: Book available — member X is next in queue")
```

**Tested by**: `BookReturnedEventIT` — verifies reservation count before/after return; verifies FIFO queue ordering across multiple reservations.

### Flow 2: Member restricted after repeated late returns

```
Member.recordLoanReturned(wasLate=true, now)
  └─ lateReturnCount++ → if > 2: status = RESTRICTED
                               → registers MemberRestrictedEvent internally

ReturnBookService
  └─ member.pullDomainEvents() → eventPublisher.publish(MemberRestrictedEvent)
          │
          └─▶ ReservationNotificationHandler
                  @TransactionalEventListener(AFTER_COMMIT)
                  └─ log.warn("Member X has been RESTRICTED")
```

**Tested by**: `ReturnBookServiceTest.thirdLateReturn_restrictseMember_andPublishesRestrictedEvent`

### Flow 3: Borrow book (cross-context orchestration)

```
BorrowBookService (lending.application)
  │
  ├─ loadMemberPort.loadMember(memberId)      ← member context
  │     member.assertCanBorrow()               (throws MemberRestrictedException or MaxLoansExceededException)
  │
  ├─ loadLoanPort.findActiveByMemberId(...)   ← lending context
  │     check no overdue loans                 (throws OverdueLoanException)
  │
  ├─ loadBookPort.loadBook(bookId)            ← catalog context
  │     book.checkAvailable()                  (throws BookNotAvailableException)
  │
  ├─ Loan.create(...)
  ├─ member.recordLoanTaken()
  ├─ book.markUnavailable()
  │
  └─ saveLoan / saveMember / saveBook         ← all three contexts, one @Transactional
```

No events published on borrow. State changes to `Book` and `Member` aggregates are the side effects.

## Trade-offs

- **Domain event publishing** — aggregates register their own events (`Loan.returnLoan()` registers `BookReturnedEvent`, `Member.recordLoanReturned()` registers `MemberRestrictedEvent`). The application service pulls and publishes them via `DomainEventPublisher` port. This keeps event origin in the domain without coupling domain to Spring. The alternative — `AbstractAggregateRoot` — only auto-publishes when the domain object is the JPA entity directly; our separate entity/domain-object design makes it a poor fit.
- **`@TransactionalEventListener(AFTER_COMMIT)`** — notification handlers fire after the transaction commits. A handler failure cannot roll back a completed return. Trade-off: events are lost if the handler throws (no retry, no dead-letter). For a production system, an outbox pattern (write event to DB in same transaction, separate process dispatches) would give at-least-once delivery.
- **Reservation delete in service, not handler** — `ReturnBookService` deletes the first reservation directly within the transaction. This is domain logic (a consequence of returning a book), not a side-effect notification. Putting it in an `@EventListener` would make it transactionally fragile and harder to reason about.
- **Cross-context port sharing** — `lending` services import `LoadBookPort` and `LoadMemberPort` from their owning contexts rather than defining lending-specific projection ports (`BookAvailabilityPort`, `MemberEligibilityPort`). Pragmatic for a monolith but couples `lending` to `catalog` and `member` port contracts.
- **Denormalised loan count** — `Member.activeLoansCount` is maintained by `lending` services, not computed by query. Fast, but a service bug can drift the count. An alternative is a `COUNT` query; accepted here for aggregate autonomy.
- **No authentication** — all endpoints are open per the spec.
