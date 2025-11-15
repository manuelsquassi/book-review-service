package com.squassi.bookreview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.squassi.bookreview.constants.ApplicationConstants;
import com.squassi.bookreview.exception.BookNotFoundException;
import com.squassi.bookreview.exception.ExternalApiException;

/**
 * Client for interacting with the Gutendex API.
 * Provides methods to search books and fetch book metadata.
 * Handles API errors and provides proper logging.
 */
@Component
public class GutendexClient {

    private static final Logger logger = LoggerFactory.getLogger(GutendexClient.class);
    
    private final RestTemplate restTemplate;

    /**
     * Constructs a GutendexClient with configured RestTemplate.
     * Sets appropriate timeouts for external API calls.
     */
    public GutendexClient(RestTemplateBuilder restTemplateBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ApplicationConstants.GutendexApi.CONNECTION_TIMEOUT_MS);
        factory.setReadTimeout(ApplicationConstants.GutendexApi.READ_TIMEOUT_MS);
        
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> factory)
                .build();
        
        logger.info("GutendexClient initialized with base URL: {}", 
                ApplicationConstants.GutendexApi.BASE_URL);
    }

    /**
     * Searches for books on Gutendex API by query string.
     *
     * @param query the search query (book title, author, etc.)
     * @return JSON string containing search results
     * @throws ExternalApiException if the API call fails
     * @throws IllegalArgumentException if query is null or blank
     */
    @SuppressWarnings("null")
    public String searchBooks(@NonNull String query) {
        if (query == null || query.isBlank()) {
            logger.warn("Attempted to search books with null or empty query");
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
        
        logger.info(ApplicationConstants.LogMessages.BOOK_SEARCH_STARTED, query);
        
        try {
            String url = String.format("%s?%s=%s", 
                    ApplicationConstants.GutendexApi.BASE_URL,
                    ApplicationConstants.GutendexApi.SEARCH_PARAM,
                    query);
            
            logger.debug("Calling Gutendex API: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            
            if (responseBody == null) {
                logger.error("Received null response body from Gutendex API");
                throw new ExternalApiException(
                        ApplicationConstants.GutendexApi.SERVICE_NAME,
                        url,
                        "Received null response body");
            }
            
            logger.debug("Successfully retrieved search results for query: {}", query);
            return responseBody;
            
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP error calling Gutendex API: {} - {}", 
                    ex.getStatusCode(), ex.getMessage());
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "search",
                    ex);
                    
        } catch (ResourceAccessException ex) {
            logger.error("Timeout or connection error calling Gutendex API: {}", ex.getMessage());
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "search",
                    "Connection timeout or network error");
                    
        } catch (Exception ex) {
            logger.error("Unexpected error calling Gutendex API: {}", ex.getMessage(), ex);
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "search",
                    ex);
        }
    }

    /**
     * Fetches detailed metadata for a specific book by its ID.
     *
     * @param bookId the Gutendex book ID
     * @return JSON string containing book metadata
     * @throws BookNotFoundException if book is not found
     * @throws ExternalApiException if the API call fails
     * @throws IllegalArgumentException if bookId is null or blank
     */
    @SuppressWarnings("null")
    public String fetchBookMetadata(@NonNull String bookId) {
        if (bookId == null || bookId.isBlank()) {
            logger.warn("Attempted to fetch book metadata with null or empty ID");
            throw new IllegalArgumentException("Book ID cannot be null or empty");
        }
        
        if (!bookId.matches(ApplicationConstants.Validation.BOOK_ID_PATTERN)) {
            logger.warn("Invalid book ID format: {}", bookId);
            throw new IllegalArgumentException("Book ID must contain only digits");
        }
        
        logger.info(ApplicationConstants.LogMessages.BOOK_METADATA_FETCHED, bookId);
        
        try {
            String url = String.format("%s/%s", 
                    ApplicationConstants.GutendexApi.BASE_URL,
                    bookId);
            
            logger.debug("Fetching metadata for book ID: {}", bookId);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            
            if (responseBody == null) {
                logger.error("Received null response body for book ID: {}", bookId);
                throw new ExternalApiException(
                        ApplicationConstants.GutendexApi.SERVICE_NAME,
                        url,
                        "Received null response body");
            }
            
            logger.debug("Successfully fetched metadata for book ID: {}", bookId);
            return responseBody;
            
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Book not found with ID: {}", bookId);
                throw new BookNotFoundException(bookId, ex);
            }
            
            logger.error("HTTP error fetching book metadata for ID {}: {} - {}", 
                    bookId, ex.getStatusCode(), ex.getMessage());
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "book/" + bookId,
                    ex);
                    
        } catch (ResourceAccessException ex) {
            logger.error("Timeout or connection error fetching book {}: {}", 
                    bookId, ex.getMessage());
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "book/" + bookId,
                    "Connection timeout or network error");
                    
        } catch (BookNotFoundException ex) {
            throw ex; // Re-throw as is
            
        } catch (Exception ex) {
            logger.error("Unexpected error fetching book metadata for ID {}: {}", 
                    bookId, ex.getMessage(), ex);
            throw new ExternalApiException(
                    ApplicationConstants.GutendexApi.SERVICE_NAME,
                    "book/" + bookId,
                    ex);
        }
    }
}
