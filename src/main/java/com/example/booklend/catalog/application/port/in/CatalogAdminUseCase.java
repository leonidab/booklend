package com.example.booklend.catalog.application.port.in;

import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.ISBN;

public interface CatalogAdminUseCase {

    record AddBookCommand(ISBN isbn, String title, String author) {}

    Book addBook(AddBookCommand command);
}
