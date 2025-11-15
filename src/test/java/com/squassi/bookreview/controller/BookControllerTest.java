package com.squassi.bookreview.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.squassi.bookreview.service.GutendexClient;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class)
@org.springframework.test.context.ContextConfiguration(classes = com.squassi.bookreview.BookReviewApplication.class)
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GutendexClient gutendexClient;

    @Test
    void searchBooksReturnsResults() throws Exception {
        String mockResponse = "{\"results\":[{\"id\":1342,\"title\":\"Pride and Prejudice\"}]}";
        
        when(gutendexClient.searchBooks("dickens")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/books/search?query=dickens"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(mockResponse));
    }

    @Test
    @SuppressWarnings("null")
    void searchBooksWithEmptyQueryReturnsBadRequest() throws Exception {
        // Empty query should return 400 Bad Request due to validation in controller
        mockMvc.perform(get("/api/books/search").param("query", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }
}
