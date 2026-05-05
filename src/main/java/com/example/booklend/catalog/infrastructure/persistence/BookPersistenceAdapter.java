package com.example.booklend.catalog.infrastructure.persistence;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;
import com.example.booklend.catalog.infrastructure.persistence.entity.BookJpaEntity;
import com.example.booklend.catalog.infrastructure.persistence.repository.BookJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "jpa", matchIfMissing = true)
public class BookPersistenceAdapter implements LoadBookPort, SaveBookPort {

    private final BookJpaRepository repository;

    public BookPersistenceAdapter(BookJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Book loadBook(BookId id) {
        return repository.findById(id.value())
                .map(this::toDomain)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Override
    public List<Book> loadAllBooks() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Book saveBook(Book book) {
        BookJpaEntity existing = repository.findById(book.getId().value()).orElse(null);
        if (existing != null) {
            existing.setAvailable(book.isAvailable());
            repository.save(existing);
        } else {
            repository.save(toJpa(book));
        }
        return book;
    }

    private Book toDomain(BookJpaEntity e) {
        return Book.reconstitute(
                new BookId(e.getId()),
                new ISBN(e.getIsbn()),
                e.getTitle(),
                e.getAuthor(),
                e.isAvailable()
        );
    }

    private BookJpaEntity toJpa(Book b) {
        return new BookJpaEntity(
                b.getId().value(),
                b.getIsbn().value(),
                b.getTitle(),
                b.getAuthor(),
                b.isAvailable()
        );
    }
}
