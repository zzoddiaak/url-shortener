package url_shortener.url_shortener.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import url_shortener.url_shortener.dto.CreateShortUrlRequest;
import url_shortener.url_shortener.dto.CreateShortUrlResponse;
import url_shortener.url_shortener.dto.UrlStatsResponse;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.service.UrlShortenerService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/shorten")
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request) {

        log.info("Creating short URL for: {}", request.getOriginalUrl());

        UrlMapping urlMapping = urlShortenerService.createShortUrl(
                request.getOriginalUrl(),
                request.getAlias(),
                request.getExpiresAt()
        );

        CreateShortUrlResponse response = CreateShortUrlResponse.builder()
                .shortUrl(urlMapping.getShortUrl())
                .originalUrl(urlMapping.getOriginalUrl())
                .alias(urlMapping.getAlias())
                .createdAt(urlMapping.getCreatedAt())
                .expiresAt(urlMapping.getExpiresAt())
                .shortUrlFull(urlShortenerService.buildShortUrl(urlMapping.getShortUrl()))
                .build();

        log.info("Created short URL: {} -> {}", response.getShortUrl(), response.getOriginalUrl());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortUrl}/stats")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable String shortUrl) {
        log.info("Getting stats for short URL: {}", shortUrl);

        UrlMapping urlMapping = urlShortenerService.getOriginalUrl(shortUrl)
                .orElseThrow(() -> new RuntimeException("URL not found: " + shortUrl));

        UrlStatsResponse response = UrlStatsResponse.builder()
                .shortUrl(urlMapping.getShortUrl())
                .originalUrl(urlMapping.getOriginalUrl())
                .alias(urlMapping.getAlias())
                .createdAt(urlMapping.getCreatedAt())
                .expiresAt(urlMapping.getExpiresAt())
                .clickCount(urlMapping.getClickCount())
                .shortUrlFull(urlShortenerService.buildShortUrl(urlMapping.getShortUrl()))
                .build();

        return ResponseEntity.ok(response);
    }
}
