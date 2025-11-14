package com.squassi.bookreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.exception.BookNotFoundException;
import com.squassi.bookreview.exception.ReviewNotFoundException;
import com.squassi.bookreview.repository.ReviewRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private GutendexClient gutendexClient;

    @Mock
    private AsyncProcessor asyncProcessor;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewRequestDto validRequest;
    private ReviewEntity savedEntity;

    @BeforeEach
    void setUp() {
        validRequest = new ReviewRequestDto();
        validRequest.setId("84");
        validRequest.setReview("Great book about Frankenstein");
        validRequest.setScore(9);

        savedEntity = new ReviewEntity();
        savedEntity.setId(1L);
        savedEntity.setBookId("84");
        savedEntity.setReviewText("Great book about Frankenstein");
        savedEntity.setScore(9);
        savedEntity.setProcessed(false);
        savedEntity.setStatus("PROCESSING");
    }

    @Test
    void createReview_ValidBookId_Success() {
        // Arrange
        when(gutendexClient.fetchBookMetadata("84")).thenReturn("{\"id\":84,\"title\":\"Frankenstein\"}");
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedEntity);
        doNothing().when(asyncProcessor).processReviewAsync(any());

        // Act
        ReviewEntity result = reviewService.createReview(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("84", result.getBookId());
        assertEquals(9, result.getScore());
        assertEquals("PROCESSING", result.getStatus());
        assertFalse(result.isProcessed());
        
        verify(gutendexClient).fetchBookMetadata("84");
        verify(reviewRepository).save(any(ReviewEntity.class));
        verify(asyncProcessor).processReviewAsync(any());
    }

    @Test
    void createReview_InvalidBookId_ThrowsException() {
        // Arrange
        when(gutendexClient.fetchBookMetadata("999999999"))
                .thenThrow(new BookNotFoundException("999999999", "Book not found"));

        validRequest.setId("999999999");

        // Act & Assert
        BookNotFoundException exception = assertThrows(BookNotFoundException.class, () -> {
            reviewService.createReview(validRequest);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(reviewRepository, never()).save(any());
        verify(asyncProcessor, never()).processReviewAsync(any());
    }

    @Test
    void getReview_ExistingId_ReturnsReview() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(savedEntity));

        // Act
        Optional<ReviewEntity> result = reviewService.getReview(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("84", result.get().getBookId());
        verify(reviewRepository).findById(1L);
    }

    @Test
    void getReview_NonExistingId_ReturnsEmpty() {
        // Arrange
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<ReviewEntity> result = reviewService.getReview(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(reviewRepository).findById(999L);
    }

    @Test
    void updateReview_ExistingId_UpdatesSuccessfully() {
        // Arrange
        ReviewRequestDto updateRequest = new ReviewRequestDto();
        updateRequest.setId("84");
        updateRequest.setReview("Updated review text");
        updateRequest.setScore(10);

        ReviewEntity existingEntity = new ReviewEntity();
        existingEntity.setId(1L);
        existingEntity.setBookId("84");
        existingEntity.setReviewText("Old review");
        existingEntity.setScore(8);

        ReviewEntity updatedEntity = new ReviewEntity();
        updatedEntity.setId(1L);
        updatedEntity.setBookId("84");
        updatedEntity.setReviewText("Updated review text");
        updatedEntity.setScore(10);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(updatedEntity);

        // Act
        ReviewEntity result = reviewService.updateReview(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getScore());
        assertEquals("Updated review text", result.getReviewText());
        verify(reviewRepository).findById(1L);
        verify(reviewRepository).save(any(ReviewEntity.class));
    }

    @Test
    void updateReview_NonExistingId_ThrowsException() {
        // Arrange
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReviewNotFoundException.class, () -> {
            reviewService.updateReview(999L, validRequest);
        });

        verify(reviewRepository).findById(999L);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_CallsRepository() {
        // Arrange
        when(reviewRepository.existsById(1L)).thenReturn(true);
        doNothing().when(reviewRepository).deleteById(1L);

        // Act
        reviewService.deleteReview(1L);

        // Assert
        verify(reviewRepository).existsById(1L);
        verify(reviewRepository).deleteById(1L);
    }
}
