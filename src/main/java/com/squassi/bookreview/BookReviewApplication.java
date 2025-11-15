package com.squassi.bookreview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for the Book Review Service.
 * Provides RESTful APIs for managing book reviews with integration to Gutendex API.
 * 
 * Features:
 * - CRUD operations for book reviews
 * - Asynchronous processing for review enrichment
 * - Integration with Gutendex API for book metadata
 * - Comprehensive logging and error handling
 * - Production-ready configuration
 * 
 * @author Book Review Team
 * @version 1.0.0
 * @since 2025
 */
@SpringBootApplication
@EnableAsync
public class BookReviewApplication {

    private static final Logger logger = LoggerFactory.getLogger(BookReviewApplication.class);

    /**
     * Main entry point for the Book Review Service application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Book Review Service...");
        
        try {
            SpringApplication.run(BookReviewApplication.class, args);
            logger.info("Book Review Service started successfully");
        } catch (Exception ex) {
            logger.error("Failed to start Book Review Service: {}", ex.getMessage(), ex);
            System.exit(1);
        }
    }
}
