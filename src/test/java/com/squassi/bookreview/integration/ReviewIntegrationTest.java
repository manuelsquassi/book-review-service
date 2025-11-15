package com.squassi.bookreview.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squassi.bookreview.BookReviewApplication;
import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.repository.ReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BookReviewApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
public class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @Test
    void fullReviewLifecycle_CreateReadUpdateDelete() throws Exception {
        // 1. Create Review
        ReviewRequestDto createRequest = new ReviewRequestDto();
        createRequest.setId("84");
        createRequest.setReview("Frankenstein is a masterpiece of gothic literature");
        createRequest.setScore(9);

        String createResponse = mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(greaterThan(0)))
                .andExpect(jsonPath("$.bookId").value("84"))
                .andExpect(jsonPath("$.score").value(9))
                .andExpect(jsonPath("$.processed").value(false))
                .andReturn().getResponse().getContentAsString();

        ReviewEntity createdReview = objectMapper.readValue(createResponse, ReviewEntity.class);
        Long reviewId = createdReview.getId();

        // 2. Read Review (while processing)
        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Review still processing"));

        // 3. Update Review
        ReviewRequestDto updateRequest = new ReviewRequestDto();
        updateRequest.setId("84");
        updateRequest.setReview("Updated: Frankenstein is an absolute masterpiece");
        updateRequest.setScore(10);

        mockMvc.perform(put("/api/reviews/" + reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(10));

        // 4. Delete Review
        mockMvc.perform(delete("/api/reviews/" + reviewId))
                .andExpect(status().isNoContent());

        // 5. Verify Deletion
        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReview_WithInvalidData_ReturnsBadRequest() throws Exception {
        ReviewRequestDto invalidRequest = new ReviewRequestDto();
        invalidRequest.setId("84");
        invalidRequest.setReview("Bad"); // Too short
        invalidRequest.setScore(15); // Too high

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchBooks_ReturnsResults() throws Exception {
        mockMvc.perform(get("/api/books/search?query=dickens"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
