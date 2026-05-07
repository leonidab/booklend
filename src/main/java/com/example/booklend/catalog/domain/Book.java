package com.example.booklend.catalog.domain;

import com.example.booklend.catalog.domain.exception.BookNotAvailableException;
import lombok.Getter;

@Getter
public class Book {

    private final BookId id;
    private final ISBN isbn;
    private final String title;
    private final String author;
    private boolean available;

    private Book(BookId id, ISBN isbn, String title, String author, boolean available) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.available = available;
    }

    public static Book create(BookId id, ISBN isbn, String title, String author) {
        return new Book(id, isbn, title, author, true);
    }

    public static Book reconstitute(BookId id, ISBN isbn, String title, String author, boolean available) {
        return new Book(id, isbn, title, author, available);
    }

    public void loanedOut() {
        if (!available) {
            throw new BookNotAvailableException(id);
        }
        this.available = false;
    }

    public void returned() {
        if (available) {
            throw new IllegalStateException("Book " + id + " is already available");
        }
        this.available = true;
    }
}
