# BookLend

Library lending service — DDD + Hexagonal Architecture.  
Stack: Java 25 · Spring Boot 4.0.6 · H2 (dev) / PostgreSQL (prod) · Maven.

## Build

```bash
./mvnw clean package -DskipTests
```

## Run — web mode

```bash
./mvnw spring-boot:run
# or
java -jar target/booklend-0.0.1-SNAPSHOT.jar
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:booklenddb`)
- Health: http://localhost:8080/actuator/health

## Run — CLI mode

```bash
java -jar target/booklend-0.0.1-SNAPSHOT.jar \
  --booklend.cli.enabled=true \
  --spring.main.web-application-type=none
```

### Commands

```
add-book <isbn> <title> <author>
add-member <name> <email>
borrow <memberId> <bookId>
return <loanId>
reserve <memberId> <bookId>
clear-restriction <memberId>
loans <memberId>
help | quit
```

### Example session

```
> add-book 9780201633610 CleanCode Martin
Book added: id=<uuid>  title=CleanCode

> add-member Alice alice@example.com
Member added: id=<uuid>  name=Alice

> borrow <memberId> <bookId>
Loan created: id=<uuid>  due=2026-05-20T10:00:00Z

> return <loanId>
Returned loan <uuid>  (late=false)
```

## Tests

```bash
./mvnw test
```

Three layers (74 tests total):
- **Domain unit** (`*Test` in `domain` packages) — pure POJO, no Spring.
- **Application unit** (`*Test` in `application/service` packages) — services + event handlers wired with `InMemory*Repository` fakes implementing the same ports as JPA adapters. No DB.
- **Integration** (`*IT`) — `@SpringBootTest` with `@ActiveProfiles("it")`. Separate H2 instance (`jdbc:h2:mem:booklend-it`) so dev seed data (`data.sql`) doesn't leak into tests. Real Spring wiring, real JPA, schema auto-generated from entities.

`HexagonalArchitectureTest` (ArchUnit) enforces layer + bounded-context boundaries.

## Architecture

DDD bounded contexts: `catalog`, `lending`, `member`, `shared`. Cross-context state changes flow as in-process domain events (`BookBorrowedEvent`, `BookReturnedEvent`) — lending publishes, catalog and member listeners react in their own context. Outbox pattern (`@TransactionalEventListener(BEFORE_COMMIT)`) captures events for downstream consumers.

See [ARCHITECTURE.md](ARCHITECTURE.md) for aggregate design, ports, and trade-offs.
