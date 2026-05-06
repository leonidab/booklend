package com.example.booklend.catalog.infrastructure.persistence;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "booklend.persistence", havingValue = "postgres")
@RequiredArgsConstructor
public class BookPostgresPersistenceAdapter implements LoadBookPort, SaveBookPort {

    private final JdbcTemplate jdbc;

    private final RowMapper<Book> rowMapper = (rs, rowNum) -> Book.reconstitute(
            new BookId(rs.getObject("id", UUID.class)),
            new ISBN(rs.getString("isbn")),
            rs.getString("title"),
            rs.getString("author"),
            rs.getBoolean("available")
    );

    @Override
    public Book loadBook(BookId id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, isbn, title, author, available FROM books WHERE id = ?",
                    rowMapper, id.value()
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BookNotFoundException(id);
        }
    }

    @Override
    public List<Book> loadAllBooks() {
        return jdbc.query("SELECT id, isbn, title, author, available FROM books", rowMapper);
    }

    @Override
    public Book saveBook(Book book) {
        jdbc.update("""
                INSERT INTO books (id, isbn, title, author, available)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET available = EXCLUDED.available
                """,
                book.getId().value(),
                book.getIsbn().value(),
                book.getTitle(),
                book.getAuthor(),
                book.isAvailable()
        );
        return book;
    }
}
