package com.example.booklend.lending.domain.exception;

import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.member.domain.MemberId;

public class BookReservedForOtherMemberException extends RuntimeException {
    public BookReservedForOtherMemberException(BookId bookId, MemberId reservedFor) {
        super("Book " + bookId + " is reserved for member " + reservedFor);
    }
}
