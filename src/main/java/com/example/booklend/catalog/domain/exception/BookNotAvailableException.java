package com.example.booklend.catalog.domain.exception;

import com.example.booklend.catalog.domain.BookId;

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(BookId bookId) {
        super("Book " + bookId + " is not available for borrowing");
    }
}
