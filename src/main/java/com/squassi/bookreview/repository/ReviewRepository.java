package com.squassi.bookreview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.squassi.bookreview.entity.ReviewEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReviewEntity database operations.
 * Provides CRUD operations and custom queries for review management.
 * 
 * @author Manuel Squassi
 * @version 1.0
 */
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    
    /**
     * Finds all reviews for a specific book ID.
     * Results are ordered by creation date descending (newest first).
     *
     * @param bookId the Gutendex book ID
     * @return list of reviews for the book, empty list if none found
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.bookId = :bookId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByBookIdOrderByCreatedAtDesc(@Param("bookId") @NonNull String bookId);
    
    /**
     * Finds all reviews with a specific status.
     *
     * @param status the review status (PROCESSING, READY, ERROR)
     * @return list of reviews with the given status
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.status = :status")
    List<ReviewEntity> findByStatus(@Param("status") @NonNull String status);
    
    /**
     * Checks if a review exists by ID.
     * Override with explicit null check.
     *
     * @param id the review ID
     * @return true if review exists, false otherwise
     */
    @Override
    boolean existsById(@NonNull Long id);
    
    /**
     * Finds a review by ID.
     * Override with explicit null handling.
     *
     * @param id the review ID
     * @return Optional containing the review if found
     */
    @Override
    @NonNull
    Optional<ReviewEntity> findById(@NonNull Long id);
}
