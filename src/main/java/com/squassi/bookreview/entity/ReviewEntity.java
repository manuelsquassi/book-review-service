package com.squassi.bookreview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a book review in the system.
 * Contains both user-provided review data and enriched metadata from external APIs.
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_book_id", columnList = "bookId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class ReviewEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String bookId;
    
    @Column(nullable = false, length = 2000)
    private String reviewText;
    
    @Column(nullable = false)
    private Integer score;
    
    @Column(length = 500)
    private String title;
    
    @Column(length = 500)
    private String authors;
    
    @Column(length = 1000)
    private String coverUrl;
    
    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    @Column(nullable = false)
    private boolean processed = false;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public ReviewEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters with null checks where appropriate
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getBookId() { 
        return bookId; 
    }
    
    public void setBookId(String bookId) { 
        this.bookId = bookId; 
    }

    public String getReviewText() { 
        return reviewText; 
    }
    
    public void setReviewText(String reviewText) { 
        this.reviewText = reviewText; 
    }

    /**
     * Alias for setReviewText for backward compatibility.
     */
    public void setReview(String review) { 
        this.reviewText = review; 
    }

    public Integer getScore() { 
        return score; 
    }
    
    public void setScore(Integer score) { 
        this.score = score; 
    }

    public String getTitle() { 
        return title; 
    }
    
    public void setTitle(String title) { 
        this.title = title; 
    }

    public String getAuthors() { 
        return authors; 
    }
    
    public void setAuthors(String authors) { 
        this.authors = authors; 
    }

    public String getCoverUrl() { 
        return coverUrl; 
    }
    
    public void setCoverUrl(String coverUrl) { 
        this.coverUrl = coverUrl; 
    }

    public String getMetadataJson() { 
        return metadataJson; 
    }
    
    public void setMetadataJson(String metadataJson) { 
        this.metadataJson = metadataJson; 
    }

    public boolean isProcessed() { 
        return processed; 
    }
    
    public void setProcessed(boolean processed) { 
        this.processed = processed; 
    }

    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }

    public Instant getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(Instant createdAt) { 
        this.createdAt = createdAt; 
    }

    public Instant getUpdatedAt() { 
        return updatedAt; 
    }
    
    public void setUpdatedAt(Instant updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewEntity that = (ReviewEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReviewEntity{" +
                "id=" + id +
                ", bookId='" + bookId + '\'' +
                ", score=" + score +
                ", status='" + status + '\'' +
                ", processed=" + processed +
                ", createdAt=" + createdAt +
                '}';
    }
}
