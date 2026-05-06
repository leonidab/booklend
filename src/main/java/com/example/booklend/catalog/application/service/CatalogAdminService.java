package com.example.booklend.catalog.application.service;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CatalogAdminService implements CatalogAdminUseCase {

    private final SaveBookPort saveBookPort;

    @Override
    public Book addBook(AddBookCommand command) {
        Book book = Book.create(BookId.newId(), command.isbn(), command.title(), command.author());
        return saveBookPort.saveBook(book);
    }
}
