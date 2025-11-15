package com.squassi.bookreview.dto;

import jakarta.validation.constraints.*;

import java.util.Objects;

import com.squassi.bookreview.constants.ApplicationConstants;

/**
 * Data Transfer Object for review creation and update requests.
 * Contains validation rules for all fields.
 */
public class ReviewRequestDto {
    
    @NotBlank(message = "Book ID is required and cannot be blank")
    @Pattern(
        regexp = ApplicationConstants.Validation.BOOK_ID_PATTERN,
        message = "Book ID must contain only digits"
    )
    private String id;

    @NotBlank(message = "Review text is required and cannot be blank")
    @Size(
        min = ApplicationConstants.Validation.REVIEW_MIN_LENGTH,
        max = ApplicationConstants.Validation.REVIEW_MAX_LENGTH,
        message = "Review text must be between " + 
                  ApplicationConstants.Validation.REVIEW_MIN_LENGTH + 
                  " and " + 
                  ApplicationConstants.Validation.REVIEW_MAX_LENGTH + 
                  " characters"
    )
    private String review;

    @NotNull(message = "Score is required")
    @Min(
        value = ApplicationConstants.Validation.SCORE_MIN_VALUE,
        message = "Score must be at least " + ApplicationConstants.Validation.SCORE_MIN_VALUE
    )
    @Max(
        value = ApplicationConstants.Validation.SCORE_MAX_VALUE,
        message = "Score must not exceed " + ApplicationConstants.Validation.SCORE_MAX_VALUE
    )
    private Integer score;

    /**
     * Default constructor.
     */
    public ReviewRequestDto() {}

    /**
     * Constructor with all fields.
     */
    public ReviewRequestDto(String id, String review, Integer score) {
        this.id = id;
        this.review = review;
        this.score = score;
    }

    // Getters and Setters
    
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

    public String getReview() { 
        return review; 
    }
    
    public void setReview(String review) { 
        this.review = review; 
    }

    public Integer getScore() { 
        return score; 
    }
    
    public void setScore(Integer score) { 
        this.score = score; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewRequestDto that = (ReviewRequestDto) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(review, that.review) &&
               Objects.equals(score, that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, review, score);
    }

    @Override
    public String toString() {
        return "ReviewRequestDto{" +
                "id='" + id + '\'' +
                ", review='" + (review != null ? review.substring(0, Math.min(50, review.length())) + "..." : "null") + '\'' +
                ", score=" + score +
                '}';
    }
}
