package com.squassi.bookreview.exception;

/**
 * Exception thrown when a book is not found on the Gutendex API.
 * This indicates that the provided book ID is invalid or the book doesn't exist.
 */
public class BookNotFoundException extends RuntimeException {
    
    private final String bookId;
    
    public BookNotFoundException(String bookId) {
        super(String.format("Book with ID '%s' not found on Gutendex API", bookId));
        this.bookId = bookId;
    }
    
    public BookNotFoundException(String bookId, String message) {
        super(message);
        this.bookId = bookId;
    }
    
    public BookNotFoundException(String bookId, Throwable cause) {
        super(String.format("Book with ID '%s' not found on Gutendex API", bookId), cause);
        this.bookId = bookId;
    }
    
    public String getBookId() {
        return bookId;
    }
}
