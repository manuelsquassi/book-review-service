package com.squassi.bookreview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squassi.bookreview.constants.ApplicationConstants;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.enums.ReviewStatus;
import com.squassi.bookreview.repository.ReviewRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Asynchronous processor for enriching reviews with book metadata.
 * Runs in a separate thread to avoid blocking the main request.
 * Fetches additional book information from Gutendex API and updates the review.
 */
@Component
public class AsyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessor.class);

    private final ReviewRepository reviewRepository;
    private final GutendexClient gutendexClient;
    private final ObjectMapper objectMapper;

    public AsyncProcessor(ReviewRepository reviewRepository, GutendexClient gutendexClient) {
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "ReviewRepository cannot be null");
        this.gutendexClient = Objects.requireNonNull(gutendexClient, "GutendexClient cannot be null");
        this.objectMapper = new ObjectMapper();
        
        logger.info("AsyncProcessor initialized successfully");
    }

    /**
     * Asynchronously processes a review to enrich it with book metadata.
     * Fetches book information from Gutendex API and updates the review entity.
     * Updates review status to READY on success or ERROR on failure.
     *
     * @param reviewId the ID of the review to process
     * @throws IllegalArgumentException if reviewId is null
     */
    @Async
    public void processReviewAsync(@NonNull Long reviewId) {
        Objects.requireNonNull(reviewId, "Review ID cannot be null");
        
        logger.info(ApplicationConstants.LogMessages.ASYNC_PROCESSING_STARTED, reviewId);
        
        try {
            // Fetch review from database
            Optional<ReviewEntity> optionalReview = reviewRepository.findById(reviewId);
            
            if (optionalReview.isEmpty()) {
                logger.warn("Review with ID {} not found for async processing", reviewId);
                return;
            }

            ReviewEntity review = optionalReview.get();
            
            if (review.getBookId() == null || review.getBookId().isBlank()) {
                logger.error("Review {} has null or empty book ID", reviewId);
                updateReviewStatus(review, ReviewStatus.ERROR);
                return;
            }
            
            logger.debug("Fetching metadata for book ID: {} (review: {})", 
                    review.getBookId(), reviewId);
            
            // Fetch book metadata from external API
            @SuppressWarnings("null")
            String metadataJson = gutendexClient.fetchBookMetadata(review.getBookId());
            
            if (metadataJson == null || metadataJson.isBlank()) {
                logger.warn("Received null or empty metadata for book ID: {}", review.getBookId());
                updateReviewStatus(review, ReviewStatus.ERROR);
                return;
            }
            
            review.setMetadataJson(metadataJson);
            
            // Parse and extract useful fields from JSON
            enrichReviewWithMetadata(review, metadataJson);
            
            // Mark as successfully processed
            review.setProcessed(true);
            review.setStatus(ReviewStatus.READY.toString());
            review.setUpdatedAt(Instant.now());
            
            reviewRepository.save(review);
            
            logger.info(ApplicationConstants.LogMessages.ASYNC_PROCESSING_COMPLETED, reviewId);
            
        } catch (Exception ex) {
            logger.error(ApplicationConstants.LogMessages.ASYNC_PROCESSING_FAILED + ": {}", 
                    reviewId, ex.getMessage(), ex);
            
            // Try to update review status to ERROR
            try {
                Optional<ReviewEntity> reviewOpt = reviewRepository.findById(reviewId);
                ReviewEntity finalReview = reviewOpt.orElse(null);
                if (finalReview != null) {
                    updateReviewStatus(finalReview, ReviewStatus.ERROR);
                }
            } catch (Exception updateEx) {
                logger.error("Failed to update review {} status to ERROR: {}", 
                        reviewId, updateEx.getMessage());
            }
        }
    }

    /**
     * Enriches a review entity with metadata extracted from JSON.
     * Extracts title, authors, and cover URL from the book metadata.
     *
     * @param review the review entity to enrich
     * @param metadataJson the JSON metadata from Gutendex API
     */
    private void enrichReviewWithMetadata(@NonNull ReviewEntity review, @NonNull String metadataJson) {
        Objects.requireNonNull(review, "Review cannot be null");
        Objects.requireNonNull(metadataJson, "Metadata JSON cannot be null");
        
        try {
            JsonNode bookData = objectMapper.readTree(metadataJson);
            
            // Extract title
            if (bookData.has("title") && !bookData.get("title").isNull()) {
                String title = bookData.get("title").asText();
                if (title != null && !title.isBlank()) {
                    review.setTitle(title);
                    logger.debug("Extracted title: {}", title);
                }
            }
            
            // Extract authors
            if (bookData.has("authors") && bookData.get("authors").isArray()) {
                StringBuilder authorsBuilder = new StringBuilder();
                JsonNode authorsArray = bookData.get("authors");
                
                for (JsonNode author : authorsArray) {
                    if (author.has("name") && !author.get("name").isNull()) {
                        String authorName = author.get("name").asText();
                        if (authorName != null && !authorName.isBlank()) {
                            if (authorsBuilder.length() > 0) {
                                authorsBuilder.append(", ");
                            }
                            authorsBuilder.append(authorName);
                        }
                    }
                }
                
                String authors = authorsBuilder.toString();
                if (!authors.isBlank()) {
                    review.setAuthors(authors);
                    logger.debug("Extracted authors: {}", authors);
                }
            }
            
            // Extract cover URL
            if (bookData.has("formats") && !bookData.get("formats").isNull()) {
                JsonNode formats = bookData.get("formats");
                if (formats.has("image/jpeg") && !formats.get("image/jpeg").isNull()) {
                    String coverUrl = formats.get("image/jpeg").asText();
                    if (coverUrl != null && !coverUrl.isBlank()) {
                        review.setCoverUrl(coverUrl);
                        logger.debug("Extracted cover URL: {}", coverUrl);
                    }
                }
            }
            
            logger.debug("Successfully enriched review {} with metadata", review.getId());
            
        } catch (Exception ex) {
            logger.error("Error parsing metadata JSON for review {}: {}", 
                    review.getId(), ex.getMessage(), ex);
            // Don't throw - partial enrichment is acceptable
        }
    }

    /**
     * Updates a review's status and saves it to the database.
     *
     * @param review the review to update
     * @param status the new status
     */
    private void updateReviewStatus(@NonNull ReviewEntity review, @NonNull ReviewStatus status) {
        Objects.requireNonNull(review, "Review cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        
        try {
            review.setStatus(status.toString());
            review.setProcessed(true);
            review.setUpdatedAt(Instant.now());
            reviewRepository.save(review);
            
            logger.debug("Updated review {} status to {}", review.getId(), status);
        } catch (Exception ex) {
            logger.error("Failed to update review {} status: {}", 
                    review.getId(), ex.getMessage(), ex);
        }
    }
}
