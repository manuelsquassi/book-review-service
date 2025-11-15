package com.squassi.bookreview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.squassi.bookreview.constants.ApplicationConstants;
import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.exception.ErrorResponse;
import com.squassi.bookreview.service.ReviewService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing book reviews.
 * Provides comprehensive CRUD operations for book reviews with proper validation,
 * error handling, and asynchronous metadata enrichment.
 * 
 * @author Manuel Squassi
 * @version 1.0
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Book review management API")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = Objects.requireNonNull(reviewService, "ReviewService cannot be null");
        logger.info("ReviewController initialized successfully");
    }

    /**
     * Creates a new review for a book.
     * Validates the book exists on Gutendex API and queues the review for asynchronous processing
     * to enrich it with book metadata (title, authors, cover image).
     *
     * @param reviewRequest the review request containing book ID, review text, and rating score
     * @return 202 Accepted with the created review entity in PROCESSING status
     * @throws com.squassi.bookreview.exception.BookNotFoundException if the book ID doesn't exist on Gutendex
     * @throws IllegalArgumentException if request validation fails
     */
    @Operation(
        summary = "Create a new book review",
        description = "Creates a new review for a book. The book ID must exist on Gutendex API. " +
                      "The review will be queued for processing to fetch additional metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Review created and queued for processing",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewEntity.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or book not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "External service unavailable",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewEntity> createReview(
            @Parameter(description = "Review data including book ID, review text, and rating", required = true)
            @Valid @RequestBody @NonNull ReviewRequestDto reviewRequest) {
        
        Objects.requireNonNull(reviewRequest, "Review request cannot be null");
        
        logger.info("Received request to create review for book ID: {} with score: {}", 
                reviewRequest.getId(), reviewRequest.getScore());
        logger.debug("Review request details: {}", reviewRequest);
        
        ReviewEntity createdReview = reviewService.createReview(reviewRequest);
        
        logger.info("Review created successfully with ID: {} for book ID: {}", 
                createdReview.getId(), createdReview.getBookId());
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(createdReview);
    }

    /**
     * Retrieves all reviews in the system.
     * Returns a list of all reviews with their current processing status.
     *
     * @return 200 OK with list of all reviews
     */
    @Operation(
        summary = "Get all reviews",
        description = "Retrieves a list of all book reviews in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all reviews",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewEntity.class)
            )
        )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReviewEntity>> getAllReviews() {
        logger.info("Received request to retrieve all reviews");
        
        List<ReviewEntity> reviews = reviewService.getAllReviews();
        
        logger.info("Successfully retrieved {} reviews", reviews.size());
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves a specific review by its ID.
     * Returns different status codes based on processing state:
     * - 200 OK: Review is ready with all metadata
     * - 202 Accepted: Review is still being processed
     * - 404 Not Found: Review doesn't exist
     *
     * @param reviewId the ID of the review to retrieve
     * @return the review entity or processing message
     * @throws com.squassi.bookreview.exception.ReviewNotFoundException if review doesn't exist
     */
    @Operation(
        summary = "Get review by ID",
        description = "Retrieves a specific review by its unique identifier. " +
                      "Returns different status codes based on processing state."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Review found and fully processed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewEntity.class)
            )
        ),
        @ApiResponse(
            responseCode = "202",
            description = "Review found but still processing",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Review not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReviewById(
            @Parameter(description = "Unique identifier of the review", required = true, example = "1")
            @PathVariable("id") @NonNull Long reviewId) {
        
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.info("Received request to retrieve review ID: {}", reviewId);
        
        Optional<ReviewEntity> reviewOptional = reviewService.getReview(reviewId);
        
        if (reviewOptional.isEmpty()) {
            logger.warn("Review not found with ID: {}", reviewId);
            return ResponseEntity.notFound().build();
        }

        ReviewEntity review = reviewOptional.get();
        
        if (!review.isProcessed()) {
            logger.debug("Review {} is still processing with status: {}", 
                    reviewId, review.getStatus());
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(ApplicationConstants.ResponseMessages.REVIEW_PROCESSING);
        }
        
        logger.info("Successfully retrieved review ID: {} for book ID: {}", 
                reviewId, review.getBookId());
        return ResponseEntity.ok(review);
    }

    /**
     * Retrieves all reviews for a specific book.
     *
     * @param bookId the Gutendex book ID
     * @return 200 OK with list of reviews for the book
     */
    @Operation(
        summary = "Get reviews by book ID",
        description = "Retrieves all reviews for a specific book using its Gutendex ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved reviews for the book",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewEntity.class)
            )
        )
    })
    @GetMapping(value = "/book/{bookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReviewEntity>> getReviewsByBookId(
            @Parameter(description = "Gutendex book ID", required = true, example = "11")
            @PathVariable("bookId") @NonNull String bookId) {
        
        Objects.requireNonNull(bookId, "Book ID cannot be null");
        
        logger.info("Received request to retrieve reviews for book ID: {}", bookId);
        
        List<ReviewEntity> reviews = reviewService.getReviewsByBookId(bookId);
        
        logger.info("Successfully retrieved {} reviews for book ID: {}", 
                reviews.size(), bookId);
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * Updates an existing review.
     * Only updates the review text and rating score, not the book metadata.
     *
     * @param reviewId the ID of the review to update
     * @param reviewRequest the updated review data
     * @return 200 OK with the updated review entity
     * @throws com.squassi.bookreview.exception.ReviewNotFoundException if review doesn't exist
     * @throws IllegalArgumentException if request validation fails
     */
    @Operation(
        summary = "Update an existing review",
        description = "Updates the review text and rating of an existing review. " +
                      "Book metadata cannot be changed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Review updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ReviewEntity.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Review not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewEntity> updateReview(
            @Parameter(description = "Unique identifier of the review to update", required = true, example = "1")
            @PathVariable("id") @NonNull Long reviewId,
            @Parameter(description = "Updated review data", required = true)
            @Valid @RequestBody @NonNull ReviewRequestDto reviewRequest) {
        
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        Objects.requireNonNull(reviewRequest, "Review request cannot be null");
        
        logger.info("Received request to update review ID: {} with new score: {}", 
                reviewId, reviewRequest.getScore());
        logger.debug("Update request details: {}", reviewRequest);
        
        ReviewEntity updatedReview = reviewService.updateReview(reviewId, reviewRequest);
        
        logger.info("Review updated successfully with ID: {} for book ID: {}", 
                reviewId, updatedReview.getBookId());
        
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Deletes a review by its ID.
     * This operation is permanent and cannot be undone.
     *
     * @param reviewId the ID of the review to delete
     * @return 204 No Content on successful deletion
     * @throws com.squassi.bookreview.exception.ReviewNotFoundException if review doesn't exist
     */
    @Operation(
        summary = "Delete a review",
        description = "Permanently deletes a review by its ID. This operation cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Review deleted successfully",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Review not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Unique identifier of the review to delete", required = true, example = "1")
            @PathVariable("id") @NonNull Long reviewId) {
        
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.info("Received request to delete review ID: {}", reviewId);
        logger.warn("Deleting review ID: {} - this operation is permanent", reviewId);
        
        reviewService.deleteReview(reviewId);
        
        logger.info("Review deleted successfully with ID: {}", reviewId);
        
        return ResponseEntity.noContent().build();
    }
}
