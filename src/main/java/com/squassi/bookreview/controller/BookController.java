package com.squassi.bookreview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.squassi.bookreview.exception.ErrorResponse;
import com.squassi.bookreview.service.GutendexClient;

import java.util.Objects;

/**
 * REST controller for book-related operations.
 * Provides endpoints to search books using the external Gutendex API.
 * 
 * @author Manuel Squassi
 * @version 1.0
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Book search API using Gutendex")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final GutendexClient gutendexClient;

    public BookController(GutendexClient gutendexClient) {
        this.gutendexClient = Objects.requireNonNull(gutendexClient, "GutendexClient cannot be null");
        logger.info("BookController initialized successfully");
    }

    /**
     * Searches for books by query string.
     * Queries the Gutendex API with the provided search term and returns matching books.
     *
     * @param query the search query (book title, author, subject, etc.)
     * @return JSON response containing search results from Gutendex API
     * @throws IllegalArgumentException if query is null or blank
     * @throws com.squassi.bookreview.exception.ExternalApiException if Gutendex API is unavailable
     */
    @Operation(
        summary = "Search for books",
        description = "Searches for books on Gutendex API by title, author, or subject. " +
                      "Returns a list of matching books with their metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved search results",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    value = "{\"count\":1,\"next\":null,\"previous\":null,\"results\":[{\"id\":11,\"title\":\"Alice's Adventures in Wonderland\",\"authors\":[{\"name\":\"Carroll, Lewis\"}]}]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search query",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Gutendex API unavailable",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE
            )
        )
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchBooks(
            @Parameter(
                description = "Search query for book title, author, or subject",
                required = true,
                example = "Alice"
            )
            @RequestParam(value = "query", required = true) @NonNull String query) {
        
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
