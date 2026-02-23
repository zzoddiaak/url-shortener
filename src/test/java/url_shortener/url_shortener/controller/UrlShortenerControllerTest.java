package url_shortener.url_shortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import url_shortener.url_shortener.dto.CreateShortUrlRequest;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.service.UrlShortenerService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @Test
    void createShortUrl_WithValidRequest_ShouldReturnCreatedUrl() throws Exception {
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl("https://example.com")
                .alias("test")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        UrlMapping urlMapping = UrlMapping.builder()
                .id(1L)
                .shortUrl("test")
                .originalUrl("https://example.com")
                .alias("test")
                .createdAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .build();

        when(urlShortenerService.createShortUrl(any(), any(), any())).thenReturn(urlMapping);
        when(urlShortenerService.buildShortUrl("test")).thenReturn("http://localhost:8080/test");

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("test"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.alias").value("test"))
                .andExpect(jsonPath("$.shortUrlFull").value("http://localhost:8080/test"));

        verify(urlShortenerService).createShortUrl(
                request.getOriginalUrl(),
                request.getAlias(),
                request.getExpiresAt()
        );
    }

    @Test
    void createShortUrl_WithInvalidUrl_ShouldReturnBadRequest() throws Exception {
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl("")
                .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originalUrl").exists());

        verify(urlShortenerService, never()).createShortUrl(any(), any(), any());
    }

    @Test
    void createShortUrl_WithDuplicateAlias_ShouldReturnBadRequest() throws Exception {
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl("https://example.com")
                .alias("duplicate")
                .build();

        when(urlShortenerService.createShortUrl(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Alias already exists: duplicate"));

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Alias already exists: duplicate"));
    }

    @Test
    void getUrlStats_WithValidShortUrl_ShouldReturnStats() throws Exception {
        String shortUrl = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl(shortUrl)
                .originalUrl("https://example.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .clickCount(5L)
                .build();

        when(urlShortenerService.getOriginalUrl(shortUrl)).thenReturn(Optional.of(urlMapping));
        when(urlShortenerService.buildShortUrl(shortUrl)).thenReturn("http://localhost:8080/" + shortUrl);

        mockMvc.perform(get("/api/v1/urls/{shortUrl}/stats", shortUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(shortUrl))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.clickCount").value(5))
                .andExpect(jsonPath("$.shortUrlFull").exists());

        verify(urlShortenerService).getOriginalUrl(shortUrl);
    }

    @Test
    void getUrlStats_WithNonExistentUrl_ShouldReturnNotFound() throws Exception {
        String shortUrl = "nonexistent";
        when(urlShortenerService.getOriginalUrl(shortUrl)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/urls/{shortUrl}/stats", shortUrl))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("URL not found: " + shortUrl));
    }
}