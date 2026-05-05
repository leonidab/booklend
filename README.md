# BookLend

A small library lending service built with DDD and Hexagonal Architecture.  
Stack: Java 25 · Spring Boot 4.0.6 · H2 (dev) / PostgreSQL (prod) · Maven.

## Build

```bash
mvn clean package -DskipTests
```

## Run (web mode)

```bash
mvn spring-boot:run
# or
java -jar target/booklend-0.0.1-SNAPSHOT.jar
```

H2 console: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:booklenddb`

## Run (CLI mode)

The CLI is the second inbound adapter for `BorrowBookUseCase`. It uses the same port interfaces as the REST controllers.

```bash
java -jar target/booklend-0.0.1-SNAPSHOT.jar \
  --booklend.cli.enabled=true \
  --spring.main.web-application-type=none
```

### CLI commands

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

### CLI example session

```
=== BookLend CLI ===
> add-book 9780201633610 CleanCode Martin
Book added: id=<uuid>  title=CleanCode

> add-member Alice alice@example.com
Member added: id=<uuid>  name=Alice

> borrow <memberId> <bookId>
Loan created: id=<uuid>  due=2026-05-12T10:00:00Z

> return <loanId>
Returned loan <uuid>  (late=false)
```

## REST API

### Books

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/books` | Add book |
| GET | `/api/books` | List all books |
| GET | `/api/books/{bookId}` | Get book |

### Members

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/members` | Add member |
| GET | `/api/members/{memberId}` | Get member |
| DELETE | `/api/members/{memberId}/restriction` | Clear RESTRICTED flag (admin) |

### Loans

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/loans` | Borrow a book |
| POST | `/api/loans/{loanId}/return` | Return a book |
| GET | `/api/loans?memberId=<id>` | Active loans for member |
| GET | `/api/loans?memberId=<id>&all=true` | All loans for member |

### Reservations

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/reservations` | Reserve a book |
| GET | `/api/reservations?memberId=<id>` | Reservations for member |

### Quick REST example

```bash
# Add a book
curl -s -X POST localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{"isbn":"9780201633610","title":"Clean Code","author":"Martin"}' | jq .

# Add a member
curl -s -X POST localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}' | jq .

# Borrow
curl -s -X POST localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"<memberId>","bookId":"<bookId>"}' | jq .

# Return
curl -s -X POST localhost:8080/api/loans/<loanId>/return | jq .
```

## Tests

```bash
mvn test
```

- **Domain unit tests** — `MemberTest`, `LoanTest`: no Spring, no JPA, pure domain logic
- **Application service tests** — `BorrowBookServiceTest`, `ReturnBookServiceTest`: use in-memory adapters, no Spring context
- **Integration tests** — `LoanControllerIT`, `BookPersistenceAdapterIT`, `BookReturnedEventIT`: full Spring context with H2

## Business rules

- Max **3 active loans** per member
- Loan due date = borrow date + **14 days**
- Members with **any overdue loan** cannot borrow until all are returned
- Members with **more than 2 late returns** are `RESTRICTED` — admin must clear via `DELETE /api/members/{id}/restriction`
- Unavailable books can be **reserved**; when returned, the first member in the queue is notified (logged) and the reservation is removed
