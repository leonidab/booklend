package com.example.booklend.catalog.domain.exception;

import com.example.booklend.catalog.domain.BookId;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(BookId bookId) {
        super("Book not found: " + bookId);
    }
}
