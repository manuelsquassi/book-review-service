package com.squassi.bookreview.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squassi.bookreview.dto.ReviewRequestDto;
import com.squassi.bookreview.entity.ReviewEntity;
import com.squassi.bookreview.service.ReviewService;

@WebMvcTest(controllers = ReviewController.class)
@org.springframework.test.context.ContextConfiguration(classes = com.squassi.bookreview.BookReviewApplication.class)
@SuppressWarnings("null")
public class ReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postValidReviewReturnsAccepted() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setId("1234");
        dto.setReview("Great book with wonderful story");
        dto.setScore(8);

        ReviewEntity entity = new ReviewEntity();
        entity.setId(1L);
        entity.setBookId("1234");
        entity.setReviewText("Great book with wonderful story");
        entity.setScore(8);
        entity.setProcessed(false);

        when(reviewService.createReview(any())).thenReturn(entity);

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void postInvalidBookIdReturnsBadRequest() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setId("999999999");
        dto.setReview("Great book");
        dto.setScore(8);

        when(reviewService.createReview(any())).thenThrow(new IllegalArgumentException("Book ID not found on Gutendex API"));

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReviewWithInvalidScoreReturnsBadRequest() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setId("1234");
        dto.setReview("Good book");
        dto.setScore(15); // Invalid score > 10

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReviewWithShortTextReturnsBadRequest() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setId("1234");
        dto.setReview("Bad"); // Too short
        dto.setScore(5);

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProcessingReviewReturns202() throws Exception {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(1L);
        entity.setProcessed(false);
        entity.setStatus("PROCESSING");

        when(reviewService.getReview(1L)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/review/1"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Review still processing"));
    }

    @Test
    void getReadyReviewReturns200() throws Exception {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(1L);
        entity.setBookId("1234");
        entity.setReviewText("Great book");
        entity.setScore(8);
        entity.setProcessed(true);
        entity.setStatus("READY");
        entity.setTitle("Test Book");
        entity.setAuthors("Author Name");

        when(reviewService.getReview(1L)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void getNonExistentReviewReturns404() throws Exception {
        when(reviewService.getReview(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/review/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReviewReturns200() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setId("1234");
        dto.setReview("Updated review text");
        dto.setScore(9);

        ReviewEntity entity = new ReviewEntity();
        entity.setId(1L);
        entity.setReviewText("Updated review text");
        entity.setScore(9);

        when(reviewService.updateReview(eq(1L), any())).thenReturn(entity);

        mockMvc.perform(put("/review/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(9));
    }

    @Test
    void deleteReviewReturns204() throws Exception {
        doNothing().when(reviewService).deleteReview(1L);

        mockMvc.perform(delete("/review/1"))
                .andExpect(status().isNoContent());
    }
}
