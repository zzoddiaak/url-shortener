package url_shortener.url_shortener.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.service.UrlShortenerService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RedirectController redirectController;

    @Test
    void redirectToOriginalUrl_WithValidShortUrl_ShouldRedirect() throws IOException {
        String shortUrl = "abc123";
        String originalUrl = "https://example.com";

        UrlMapping urlMapping = UrlMapping.builder()
                .id(1L)
                .shortUrl(shortUrl)
                .originalUrl(originalUrl)
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        when(urlShortenerService.getOriginalUrl(shortUrl)).thenReturn(Optional.of(urlMapping));

        assertDoesNotThrow(() -> redirectController.redirectToOriginalUrl(shortUrl, response));

        verify(response).sendRedirect(originalUrl);
        verify(urlShortenerService).getOriginalUrl(shortUrl);
    }

    @Test
    void redirectToOriginalUrl_WithExpiredUrl_ShouldReturnGone() throws IOException {
        String shortUrl = "expired";
        UrlMapping expiredMapping = UrlMapping.builder()
                .shortUrl(shortUrl)
                .originalUrl("https://expired.com")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(urlShortenerService.getOriginalUrl(shortUrl)).thenReturn(Optional.of(expiredMapping));

        redirectController.redirectToOriginalUrl(shortUrl, response);

        verify(response).sendError(410, "URL has expired");
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void redirectToOriginalUrl_WithNonExistentUrl_ShouldThrowException() {
        String shortUrl = "nonexistent";
        when(urlShortenerService.getOriginalUrl(shortUrl)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> redirectController.redirectToOriginalUrl(shortUrl, response));

        assertEquals("URL not found: nonexistent", exception.getMessage());
        verify(urlShortenerService).getOriginalUrl(shortUrl);
    }

    @Test
    void redirectToOriginalUrl_WithAlias_ShouldRedirect() throws IOException {
        String alias = "myalias";
        String originalUrl = "https://alias.com";

        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl(alias)
                .originalUrl(originalUrl)
                .alias(alias)
                .build();

        when(urlShortenerService.getOriginalUrl(alias)).thenReturn(Optional.of(urlMapping));

        redirectController.redirectToOriginalUrl(alias, response);

        verify(response).sendRedirect(originalUrl);
    }
}