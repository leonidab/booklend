package com.example.booklend.catalog.application.service;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.shared.domain.event.BookBorrowedEvent;
import com.example.booklend.shared.domain.event.BookReturnedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookLoanEventHandler {

    private final LoadBookPort loadBookPort;
    private final SaveBookPort saveBookPort;

    @EventListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void onBookBorrowed(BookBorrowedEvent event) {
        Book book = loadBookPort.loadBook(new BookId(event.bookId()));
        book.loanedOut();
        saveBookPort.saveBook(book);
    }

    @EventListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void onBookReturned(BookReturnedEvent event) {
        Book book = loadBookPort.loadBook(new BookId(event.bookId()));
        book.returned();
        saveBookPort.saveBook(book);
    }
}
