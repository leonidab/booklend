package com.example.booklend.shared.infrastructure.inmemory;

import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.application.port.out.SaveBookPort;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.exception.BookNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements LoadBookPort, SaveBookPort {

    private final Map<BookId, Book> store = new ConcurrentHashMap<>();

    public void save(Book book) { store.put(book.getId(), book); }

    @Override
    public Book loadBook(BookId id) {
        Book book = store.get(id);
        if (book == null) throw new BookNotFoundException(id);
        return book;
    }

    @Override
    public List<Book> loadAllBooks() { return new ArrayList<>(store.values()); }

    @Override
    public Book saveBook(Book book) { store.put(book.getId(), book); return book; }
}
