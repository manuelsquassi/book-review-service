package com.squassi.bookreview.exception;

/**
 * Exception thrown when a review is not found in the system.
 * This is a custom runtime exception for better error handling and clarity.
 */
public class ReviewNotFoundException extends RuntimeException {
    
    private final Long reviewId;
    
    public ReviewNotFoundException(Long reviewId) {
        super(String.format("Review with ID %d not found", reviewId));
        this.reviewId = reviewId;
    }
    
    public ReviewNotFoundException(Long reviewId, String message) {
        super(message);
        this.reviewId = reviewId;
    }
    
    public Long getReviewId() {
        return reviewId;
    }
}
