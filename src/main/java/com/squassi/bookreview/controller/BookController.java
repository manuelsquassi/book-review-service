package com.squassi.bookreview.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.squassi.bookreview.service.GutendexClient;

import java.util.Objects;

/**
 * REST controller for book-related operations.
 * Provides endpoints to search books using the Gutendex API.
 */
@RestController
@RequestMapping("/book")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final GutendexClient gutendexClient;

    public BookController(GutendexClient gutendexClient) {
        this.gutendexClient = Objects.requireNonNull(gutendexClient, "GutendexClient cannot be null");
        logger.info("BookController initialized successfully");
    }

    /**
     * Searches for books by query string.
     * Queries the Gutendex API with the provided search term.
     *
     * @param query the search query (book title, author, etc.)
     * @return JSON response containing search results
     */
    @SuppressWarnings("null")
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchBooks(
            @RequestParam(value = "q", required = true) @NonNull String query) {
        
        Objects.requireNonNull(query, "Search query cannot be null");
        
        if (query.isBlank()) {
            logger.warn("Received empty search query");
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Search query cannot be empty\"}");
        }
        
        logger.info("Searching books with query: {}", query);
        
        String searchResults = gutendexClient.searchBooks(query);
        
        logger.debug("Book search completed for query: {}", query);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(searchResults);
    }
}
