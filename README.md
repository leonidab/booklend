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

Domain unit tests (no Spring), application service tests (in-memory adapters), integration tests (full Spring context + H2).
