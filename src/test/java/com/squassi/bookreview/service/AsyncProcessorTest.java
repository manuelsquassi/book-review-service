package com.squassi.bookreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.repository.ReviewRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class AsyncProcessorTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private GutendexClient gutendexClient;

    @InjectMocks
    private AsyncProcessor asyncProcessor;

    private ReviewEntity reviewEntity;

    @BeforeEach
    void setUp() {
        reviewEntity = new ReviewEntity();
        reviewEntity.setId(1L);
        reviewEntity.setBookId("84");
        reviewEntity.setReviewText("Great book");
        reviewEntity.setScore(9);
        reviewEntity.setProcessed(false);
        reviewEntity.setStatus("PROCESSING");
    }

    @Test
    void processReviewAsync_Success_EnrichesReviewData() {
        // Arrange
        String mockMetadata = "{\"id\":84,\"title\":\"Frankenstein\",\"authors\":[{\"name\":\"Shelley, Mary Wollstonecraft\"}],\"formats\":{\"image/jpeg\":\"https://example.com/cover.jpg\"}}";
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(reviewEntity));
        when(gutendexClient.fetchBookMetadata("84")).thenReturn(mockMetadata);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);

        // Act
        asyncProcessor.processReviewAsync(1L);

        // Assert
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository).save(captor.capture());
        
        ReviewEntity savedEntity = captor.getValue();
        assertTrue(savedEntity.isProcessed());
        assertEquals("READY", savedEntity.getStatus());
        assertEquals("Frankenstein", savedEntity.getTitle());
        assertEquals("Shelley, Mary Wollstonecraft", savedEntity.getAuthors());
        assertEquals("https://example.com/cover.jpg", savedEntity.getCoverUrl());
        assertNotNull(savedEntity.getMetadataJson());
    }

    @Test
    void processReviewAsync_ApiError_SetsErrorStatus() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(reviewEntity));
        when(gutendexClient.fetchBookMetadata("84")).thenThrow(new RuntimeException("API Error"));
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);

        // Act
        asyncProcessor.processReviewAsync(1L);

        // Assert
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository).save(captor.capture());
        
        ReviewEntity savedEntity = captor.getValue();
        assertTrue(savedEntity.isProcessed());
        assertEquals("ERROR", savedEntity.getStatus());
    }

    @Test
    void processReviewAsync_ReviewNotFound_DoesNothing() {
        // Arrange
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        asyncProcessor.processReviewAsync(999L);

        // Assert
        verify(reviewRepository).findById(999L);
        verify(gutendexClient, never()).fetchBookMetadata(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void processReviewAsync_WithoutAuthors_HandlesGracefully() {
        // Arrange
        String mockMetadata = "{\"id\":84,\"title\":\"Frankenstein\",\"formats\":{\"image/jpeg\":\"https://example.com/cover.jpg\"}}";
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(reviewEntity));
        when(gutendexClient.fetchBookMetadata("84")).thenReturn(mockMetadata);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);

        // Act
        asyncProcessor.processReviewAsync(1L);

        // Assert
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository).save(captor.capture());
        
        ReviewEntity savedEntity = captor.getValue();
        assertTrue(savedEntity.isProcessed());
        assertEquals("READY", savedEntity.getStatus());
        assertEquals("Frankenstein", savedEntity.getTitle());
    }
}
