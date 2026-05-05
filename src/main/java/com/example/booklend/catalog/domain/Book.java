package com.example.booklend.catalog.domain;

import com.example.booklend.catalog.domain.exception.BookNotAvailableException;

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

    public void checkAvailable() {
        if (!available) {
            throw new BookNotAvailableException(id);
        }
    }

    public void markUnavailable() {
        this.available = false;
    }

    public void markAvailable() {
        this.available = true;
    }

    public BookId getId() { return id; }
    public ISBN getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isAvailable() { return available; }
}
