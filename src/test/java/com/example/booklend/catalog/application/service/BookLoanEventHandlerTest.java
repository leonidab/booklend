package com.example.booklend.catalog.application.service;

import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.catalog.domain.exception.BookNotAvailableException;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
import com.example.booklend.shared.infrastructure.inmemory.InMemoryBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookLoanEventHandlerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryBookRepository bookRepo;
    private BookLoanEventHandler handler;

    @BeforeEach
    void setUp() {
        bookRepo = new InMemoryBookRepository();
        handler = new BookLoanEventHandler(bookRepo, bookRepo);
    }

    private Book savedAvailableBook() {
        Book b = Book.create(BookId.newId(), new ISBN("9780201633610"), "Clean Code", "Martin");
        bookRepo.save(b);
        return b;
    }

    @Test
    void onBookBorrowed_marksBookUnavailable() {
        Book book = savedAvailableBook();

        handler.onBookBorrowed(new BookBorrowedEvent(
                book.getId().value(), UUID.randomUUID(), UUID.randomUUID(), NOW));

        assertThat(bookRepo.loadBook(book.getId()).isAvailable()).isFalse();
    }

    @Test
    void onBookBorrowed_throwsBookNotAvailable_whenAlreadyLoaned() {
        Book book = Book.reconstitute(BookId.newId(), new ISBN("9780201633610"), "T", "A", false);
        bookRepo.save(book);

        assertThatThrownBy(() -> handler.onBookBorrowed(new BookBorrowedEvent(
                book.getId().value(), UUID.randomUUID(), UUID.randomUUID(), NOW)))
                .isInstanceOf(BookNotAvailableException.class);
    }

    @Test
    void onBookBorrowed_throwsBookNotFound_whenBookMissing() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> handler.onBookBorrowed(new BookBorrowedEvent(
                unknown, UUID.randomUUID(), UUID.randomUUID(), NOW)))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void onBookReturned_marksBookAvailable() {
        Book book = Book.reconstitute(BookId.newId(), new ISBN("9780201633610"), "T", "A", false);
        bookRepo.save(book);

        handler.onBookReturned(new BookReturnedEvent(
                book.getId().value(), UUID.randomUUID(), UUID.randomUUID(), false, NOW));

        assertThat(bookRepo.loadBook(book.getId()).isAvailable()).isTrue();
    }

    @Test
    void onBookReturned_throwsIllegalState_whenAlreadyAvailable() {
        Book book = savedAvailableBook();

        assertThatThrownBy(() -> handler.onBookReturned(new BookReturnedEvent(
                book.getId().value(), UUID.randomUUID(), UUID.randomUUID(), false, NOW)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onBookReturned_throwsBookNotFound_whenBookMissing() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> handler.onBookReturned(new BookReturnedEvent(
                unknown, UUID.randomUUID(), UUID.randomUUID(), false, NOW)))
                .isInstanceOf(BookNotFoundException.class);
    }
}
