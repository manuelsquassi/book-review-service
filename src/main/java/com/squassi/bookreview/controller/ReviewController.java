package com.squassi.bookreview.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.squassi.bookreview.constants.ApplicationConstants;
import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.service.ReviewService;

import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for review management.
 * Provides CRUD operations for book reviews.
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = Objects.requireNonNull(reviewService, "ReviewService cannot be null");
        logger.info("ReviewController initialized successfully");
    }

    /**
     * Creates a new review for a book.
     * Validates the book exists and queues the review for processing.
     *
     * @param request the review request containing book ID, text, and score
     * @return 202 Accepted with the created review entity
     */
    @PostMapping
    public ResponseEntity<ReviewEntity> createReview(
            @Valid @RequestBody @NonNull ReviewRequestDto request) {
        
        Objects.requireNonNull(request, "Review request cannot be null");
        
        logger.info("Received request to create review for book ID: {}", request.getId());
        
        ReviewEntity createdReview = reviewService.createReview(request);
        
        logger.info("Review created successfully with ID: {}", createdReview.getId());
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(createdReview);
    }

    /**
     * Retrieves a review by its ID.
     * Returns different status codes based on processing state:
     * - 200 OK: Review is ready
     * - 202 Accepted: Review is still processing
     * - 404 Not Found: Review doesn't exist
     *
     * @param reviewId the ID of the review to retrieve
     * @return the review entity or processing message
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReview(
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
            logger.debug("Review {} is still processing", reviewId);
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(ApplicationConstants.ResponseMessages.REVIEW_PROCESSING);
        }
        
        logger.info("Successfully retrieved review ID: {}", reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Updates an existing review.
     * Only updates the review text and score.
     *
     * @param reviewId the ID of the review to update
     * @param request the updated review data
     * @return 200 OK with the updated review entity
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewEntity> updateReview(
            @PathVariable("id") @NonNull Long reviewId,
            @Valid @RequestBody @NonNull ReviewRequestDto request) {
        
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        Objects.requireNonNull(request, "Review request cannot be null");
        
        logger.info("Received request to update review ID: {}", reviewId);
        
        ReviewEntity updatedReview = reviewService.updateReview(reviewId, request);
        
        logger.info("Review updated successfully with ID: {}", reviewId);
        
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Deletes a review by its ID.
     *
     * @param reviewId the ID of the review to delete
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("id") @NonNull Long reviewId) {
        
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.info("Received request to delete review ID: {}", reviewId);
        
        reviewService.deleteReview(reviewId);
        
        logger.info("Review deleted successfully with ID: {}", reviewId);
        
        return ResponseEntity.noContent().build();
    }
}
