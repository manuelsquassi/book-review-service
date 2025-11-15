package com.squassi.bookreview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.squassi.bookreview.constants.ApplicationConstants;
import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.enums.ReviewStatus;
import com.squassi.bookreview.exception.BookNotFoundException;
import com.squassi.bookreview.exception.ReviewNotFoundException;
import com.squassi.bookreview.repository.ReviewRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for managing book reviews.
 * Handles business logic for creating, retrieving, updating, and deleting reviews.
 * Coordinates with external API to validate and enrich review data.
 */
@Service
@Transactional
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final GutendexClient gutendexClient;
    private final AsyncProcessor asyncProcessor;

    public ReviewService(
            ReviewRepository reviewRepository, 
            GutendexClient gutendexClient, 
            AsyncProcessor asyncProcessor) {
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "ReviewRepository cannot be null");
        this.gutendexClient = Objects.requireNonNull(gutendexClient, "GutendexClient cannot be null");
        this.asyncProcessor = Objects.requireNonNull(asyncProcessor, "AsyncProcessor cannot be null");
        
        logger.info("ReviewService initialized successfully");
    }

    /**
     * Creates a new review for a book.
     * Validates the book exists on Gutendex API before creating the review.
     * Queues the review for asynchronous processing to fetch additional metadata.
     *
     * @param request the review request containing book ID, review text, and score
     * @return the created review entity with PROCESSING status
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws BookNotFoundException if the book ID doesn't exist on Gutendex API
     */
    public ReviewEntity createReview(@NonNull ReviewRequestDto request) {
        Objects.requireNonNull(request, "Review request cannot be null");
        Objects.requireNonNull(request.getId(), "Book ID cannot be null");
        Objects.requireNonNull(request.getReview(), "Review text cannot be null");
        Objects.requireNonNull(request.getScore(), "Review score cannot be null");
        
        logger.info("Creating review for book ID: {}", request.getId());
        
        // Verify book exists by calling external API
        try {
            logger.debug("Validating book existence for ID: {}", request.getId());
            @SuppressWarnings("null")
            String metadata = gutendexClient.fetchBookMetadata(request.getId());
            if (metadata == null) {
                throw new BookNotFoundException(request.getId());
            }
        } catch (BookNotFoundException ex) {
            logger.warn("Attempted to create review for non-existent book ID: {}", request.getId());
            throw ex;
        } catch (Exception ex) {
            logger.error("Error validating book ID {} on Gutendex API: {}", 
                    request.getId(), ex.getMessage());
            throw new BookNotFoundException(request.getId(), 
                    "Unable to validate book ID. Please try again later.");
        }

        // Create review entity
        ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setBookId(request.getId());
        reviewEntity.setReview(request.getReview());
        reviewEntity.setScore(request.getScore());
        reviewEntity.setProcessed(false);
        reviewEntity.setStatus(ReviewStatus.PROCESSING.toString());
        reviewEntity.setCreatedAt(Instant.now());
        reviewEntity.setUpdatedAt(Instant.now());
        
        // Save to database
        ReviewEntity savedReview = reviewRepository.save(reviewEntity);
        logger.info(ApplicationConstants.LogMessages.REVIEW_CREATED, savedReview.getId());

        // Trigger async processing
        try {
            Long reviewId = savedReview.getId();
            if (reviewId != null) {
                asyncProcessor.processReviewAsync(reviewId);
                logger.debug("Queued review {} for async processing", reviewId);
            }
        } catch (Exception ex) {
            logger.error("Failed to queue review {} for async processing: {}", 
                    savedReview.getId(), ex.getMessage(), ex);
            // Don't fail the request - processing will be retried or handled separately
        }
        
        return savedReview;
    }

    /**
     * Retrieves a review by its ID.
     *
     * @param reviewId the ID of the review to retrieve
     * @return Optional containing the review if found, empty otherwise
     * @throws IllegalArgumentException if reviewId is null
     */
    @Transactional(readOnly = true)
    public Optional<ReviewEntity> getReview(@NonNull Long reviewId) {
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.debug("Retrieving review with ID: {}", reviewId);
        
        Optional<ReviewEntity> review = reviewRepository.findById(reviewId);
        
        if (review.isPresent()) {
            logger.info(ApplicationConstants.LogMessages.REVIEW_RETRIEVED, reviewId);
        } else {
            logger.warn(ApplicationConstants.LogMessages.REVIEW_NOT_FOUND, reviewId);
        }
        
        return review;
    }

    /**
     * Updates an existing review.
     * Only updates the review text and score, not the book metadata.
     *
     * @param reviewId the ID of the review to update
     * @param request the updated review data
     * @return the updated review entity
     * @throws ReviewNotFoundException if the review doesn't exist
     * @throws IllegalArgumentException if parameters are null or invalid
     */
    public ReviewEntity updateReview(@NonNull Long reviewId, @NonNull ReviewRequestDto request) {
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        Objects.requireNonNull(request, "Review request cannot be null");
        Objects.requireNonNull(request.getReview(), "Review text cannot be null");
        Objects.requireNonNull(request.getScore(), "Review score cannot be null");
        
        logger.info("Updating review with ID: {}", reviewId);
        
        ReviewEntity existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("Attempted to update non-existent review ID: {}", reviewId);
                    return new ReviewNotFoundException(reviewId);
                });
        
        // Update mutable fields
        existingReview.setReview(request.getReview());
        existingReview.setScore(request.getScore());
        existingReview.setUpdatedAt(Instant.now());
        
        ReviewEntity updatedReview = reviewRepository.save(existingReview);
        logger.info(ApplicationConstants.LogMessages.REVIEW_UPDATED, reviewId);
        
        return updatedReview;
    }

    /**
     * Retrieves all reviews in the system.
     *
     * @return list of all reviews, ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<ReviewEntity> getAllReviews() {
        logger.debug("Retrieving all reviews");
        
        List<ReviewEntity> reviews = reviewRepository.findAll();
        
        logger.info("Retrieved {} total reviews", reviews.size());
        
        return reviews;
    }

    /**
     * Retrieves all reviews for a specific book ID.
     *
     * @param bookId the Gutendex book ID
     * @return list of reviews for the book, ordered by creation date descending
     * @throws IllegalArgumentException if bookId is null or blank
     */
    @Transactional(readOnly = true)
    public List<ReviewEntity> getReviewsByBookId(@NonNull String bookId) {
        if (bookId == null || bookId.isBlank()) {
            logger.warn("Attempted to retrieve reviews with null or empty book ID");
            throw new IllegalArgumentException("Book ID cannot be null or empty");
        }
        
        logger.debug("Retrieving reviews for book ID: {}", bookId);
        
        List<ReviewEntity> reviews = reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        
        logger.info("Found {} reviews for book ID: {}", reviews.size(), bookId);
        
        return reviews;
    }

    /**
     * Deletes a review by its ID.
     *
     * @param reviewId the ID of the review to delete
     * @throws IllegalArgumentException if reviewId is null
     * @throws ReviewNotFoundException if review doesn't exist
     */
    public void deleteReview(@NonNull Long reviewId) {
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.info("Deleting review with ID: {}", reviewId);
        
        // Check if review exists before deleting
        if (!reviewRepository.existsById(reviewId)) {
            logger.warn("Attempted to delete non-existent review ID: {}", reviewId);
            throw new ReviewNotFoundException(reviewId);
        }
        
        reviewRepository.deleteById(reviewId);
        logger.info(ApplicationConstants.LogMessages.REVIEW_DELETED, reviewId);
    }
}
