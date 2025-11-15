package com.squassi.bookreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class GutendexClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private GutendexClient gutendexClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        gutendexClient = new GutendexClient(restTemplateBuilder);
        
        // Inject mock RestTemplate via reflection
        try {
            var field = GutendexClient.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(gutendexClient, restTemplate);
        } catch (Exception e) {
            fail("Failed to inject mock RestTemplate: " + e.getMessage());
        }
    }

    @Test
    void searchBooks_ReturnsResults() {
        // Arrange
        String mockResponse = "{\"results\":[{\"id\":1342,\"title\":\"Pride and Prejudice\"}]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        String result = gutendexClient.searchBooks("dickens");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Pride and Prejudice"));
        verify(restTemplate).getForEntity(contains("search=dickens"), eq(String.class));
    }

    @Test
    void fetchBookMetadata_ReturnsBookData() {
        // Arrange
        String mockResponse = "{\"id\":84,\"title\":\"Frankenstein\",\"authors\":[{\"name\":\"Shelley, Mary\"}]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        String result = gutendexClient.fetchBookMetadata("84");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Frankenstein"));
        assertTrue(result.contains("Shelley, Mary"));
        verify(restTemplate).getForEntity(contains("/84"), eq(String.class));
    }

    @Test
    void searchBooks_WithEmptyQuery_ThrowsException() {
        // Arrange - empty query should throw IllegalArgumentException
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            gutendexClient.searchBooks("");
        });
        
        // Verify no API call was made
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
    }
}
