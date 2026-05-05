package com.example.booklend.catalog.application.port.out;

import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;

import java.util.List;

public interface LoadBookPort {
    Book loadBook(BookId id);
    List<Book> loadAllBooks();
}
