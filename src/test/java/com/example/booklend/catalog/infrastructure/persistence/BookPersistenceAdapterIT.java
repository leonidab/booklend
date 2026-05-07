package com.example.booklend.catalog.infrastructure.persistence;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("it")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookPersistenceAdapterIT {

    @Autowired LoadBookPort loadBookPort;
    @Autowired SaveBookPort saveBookPort;

    @Test
    void saveAndLoadBook_roundTrip() {
        Book book = Book.create(BookId.newId(), new ISBN("9780132350884"), "Clean Code", "Martin");
        saveBookPort.saveBook(book);

        Book loaded = loadBookPort.loadBook(book.getId());

        assertThat(loaded.getId()).isEqualTo(book.getId());
        assertThat(loaded.getIsbn()).isEqualTo(book.getIsbn());
        assertThat(loaded.getTitle()).isEqualTo("Clean Code");
        assertThat(loaded.isAvailable()).isTrue();
    }

    @Test
    void updateBook_persistsAvailabilityChange() {
        Book book = Book.create(BookId.newId(), new ISBN("9780132350884"), "Test Book", "Author");
        saveBookPort.saveBook(book);

        book.loanedOut();
        saveBookPort.saveBook(book);

        assertThat(loadBookPort.loadBook(book.getId()).isAvailable()).isFalse();
    }

    @Test
    void loadAllBooks_returnsAllSaved() {
        saveBookPort.saveBook(Book.create(BookId.newId(), new ISBN("9780132350884"), "Book A", "Author"));
        saveBookPort.saveBook(Book.create(BookId.newId(), new ISBN("9780201633610"), "Book B", "Author"));

        List<Book> all = loadBookPort.loadAllBooks();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void loadBook_throwsWhenNotFound() {
        BookId missing = BookId.newId();
        assertThatThrownBy(() -> loadBookPort.loadBook(missing))
                .isInstanceOf(BookNotFoundException.class);
    }
}
