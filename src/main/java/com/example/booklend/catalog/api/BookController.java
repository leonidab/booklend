package com.example.booklend.catalog.api;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.application.port.out.LoadBookPort;
import com.example.booklend.catalog.api.dto.CreateBookRequest;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final CatalogAdminUseCase catalogAdminUseCase;
    private final LoadBookPort loadBookPort;

    public BookController(CatalogAdminUseCase catalogAdminUseCase, LoadBookPort loadBookPort) {
        this.catalogAdminUseCase = catalogAdminUseCase;
        this.loadBookPort = loadBookPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookResponse addBook(@RequestBody CreateBookRequest request) {
        Book book = catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN(request.isbn()), request.title(), request.author()));
        return BookResponse.from(book);
    }

    @GetMapping
    List<BookResponse> listBooks() {
        return loadBookPort.loadAllBooks().stream().map(BookResponse::from).toList();
    }

    @GetMapping("/{bookId}")
    BookResponse getBook(@PathVariable String bookId) {
        Book book = loadBookPort.loadBook(BookId.of(bookId));
        return BookResponse.from(book);
    }

    record BookResponse(String id, String isbn, String title, String author, boolean available) {
        static BookResponse from(Book book) {
            return new BookResponse(
                    book.getId().toString(),
                    book.getIsbn().toString(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.isAvailable()
            );
        }
    }
}
