package com.squassi.bookreview.constants;

/**
 * Application-wide constants for the Book Review Service.
 * Centralizes all magic strings and numbers for better maintainability.
 */
public final class ApplicationConstants {
    
    private ApplicationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Gutendex API related constants
     */
    public static final class GutendexApi {
        public static final String BASE_URL = "https://gutendex.com/books";
        public static final String SEARCH_PARAM = "search";
        public static final int CONNECTION_TIMEOUT_MS = 5000;
        public static final int READ_TIMEOUT_MS = 10000;
        public static final String SERVICE_NAME = "Gutendex";
        
        private GutendexApi() {}
    }
    
    /**
     * Validation constants
     */
    public static final class Validation {
        public static final int REVIEW_MIN_LENGTH = 5;
        public static final int REVIEW_MAX_LENGTH = 2000;
        public static final int SCORE_MIN_VALUE = 1;
        public static final int SCORE_MAX_VALUE = 10;
        public static final String BOOK_ID_PATTERN = "^[0-9]+$";
        
        private Validation() {}
    }
    
    /**
     * Log messages
     */
    public static final class LogMessages {
        public static final String REVIEW_CREATED = "Review created successfully with ID: {}";
        public static final String REVIEW_RETRIEVED = "Retrieved review with ID: {}";
        public static final String REVIEW_UPDATED = "Review updated successfully with ID: {}";
        public static final String REVIEW_DELETED = "Review deleted successfully with ID: {}";
        public static final String REVIEW_NOT_FOUND = "Review not found with ID: {}";
        public static final String BOOK_SEARCH_STARTED = "Searching books with query: {}";
        public static final String BOOK_METADATA_FETCHED = "Fetched metadata for book ID: {}";
        public static final String ASYNC_PROCESSING_STARTED = "Started async processing for review ID: {}";
        public static final String ASYNC_PROCESSING_COMPLETED = "Completed async processing for review ID: {}";
        public static final String ASYNC_PROCESSING_FAILED = "Failed async processing for review ID: {}";
        public static final String EXTERNAL_API_ERROR = "Error calling external API: {}";
        
        private LogMessages() {}
    }
    
    /**
     * Response messages
     */
    public static final class ResponseMessages {
        public static final String REVIEW_PROCESSING = "Review still processing";
        public static final String REVIEW_CREATED_SUCCESS = "Review created and queued for processing";
        public static final String REVIEW_UPDATED_SUCCESS = "Review updated successfully";
        public static final String REVIEW_DELETED_SUCCESS = "Review deleted successfully";
        
        private ResponseMessages() {}
    }
}
