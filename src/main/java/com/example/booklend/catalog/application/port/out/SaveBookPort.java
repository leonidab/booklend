package com.example.booklend.catalog.application.port.out;

import com.example.booklend.catalog.domain.Book;

public interface SaveBookPort {
    Book saveBook(Book book);
}
