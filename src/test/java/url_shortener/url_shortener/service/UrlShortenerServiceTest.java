package url_shortener.url_shortener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.repository.UrlMappingRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @Test
    void generateShortUrl_ShouldReturnStringOfCorrectLength() {
        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(6, shortUrl.length());
        assertTrue(shortUrl.matches("[A-Za-z0-9]{6}"));
    }

    @Test
    void createShortUrl_WithAlias_ShouldUseAliasAsShortUrl() {
        String originalUrl = "https://example.com";
        String alias = "myalias";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        when(urlMappingRepository.existsByAlias(alias)).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            mapping.setId(1L);
            return mapping;
        });

        UrlMapping result = urlShortenerService.createShortUrl(originalUrl, alias, expiresAt);

        assertNotNull(result);
        assertEquals(alias, result.getShortUrl());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals(alias, result.getAlias());
        assertEquals(expiresAt, result.getExpiresAt());

        verify(urlMappingRepository).existsByAlias(alias);
        verify(urlMappingRepository).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_WithoutAlias_ShouldGenerateShortUrl() {
        String originalUrl = "https://example.com";

        when(urlMappingRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            mapping.setId(1L);
            return mapping;
        });

        UrlMapping result = urlShortenerService.createShortUrl(originalUrl, null, null);

        assertNotNull(result);
        assertEquals(6, result.getShortUrl().length());
        assertNull(result.getAlias());
        assertNull(result.getExpiresAt());

        verify(urlMappingRepository, atLeastOnce()).existsByShortUrl(anyString());
        verify(urlMappingRepository).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_WithExistingAlias_ShouldThrowException() {
        String originalUrl = "https://example.com";
        String alias = "existing-alias";

        when(urlMappingRepository.existsByAlias(alias)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> urlShortenerService.createShortUrl(originalUrl, alias, null));

        assertEquals("Alias already exists: " + alias, exception.getMessage());
        verify(urlMappingRepository).existsByAlias(alias);
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    void getOriginalUrl_WithValidShortUrl_ShouldReturnUrlMappingAndIncrementCount() {
        String shortUrl = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
                .id(1L)
                .shortUrl(shortUrl)
                .originalUrl("https://example.com")
                .clickCount(0L)
                .build();

        when(urlMappingRepository.findActiveByShortUrlOrAlias(shortUrl, LocalDateTime.now()))
                .thenReturn(Optional.of(urlMapping));

        Optional<UrlMapping> result = urlShortenerService.getOriginalUrl(shortUrl);

        assertTrue(result.isPresent());
        assertEquals(urlMapping, result.get());
        assertEquals(1L, urlMapping.getClickCount()); // Incremented

        verify(urlMappingRepository).findActiveByShortUrlOrAlias(shortUrl, LocalDateTime.now());
        verify(urlMappingRepository).incrementClickCount(1L);
    }

    @Test
    void buildShortUrl_ShouldReturnFullUrl() {
        String shortUrl = "abc123";

        String result = urlShortenerService.buildShortUrl(shortUrl);

        assertEquals("http://localhost:8080/" + shortUrl, result);
    }
}