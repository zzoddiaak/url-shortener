package url_shortener.url_shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.repository.UrlMappingRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_URL_LENGTH = 6;
    private final Random random = new Random();

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final UrlMappingRepository urlMappingRepository;

    public String generateShortUrl() {
        StringBuilder shortUrl = new StringBuilder();
        for (int i = 0; i < SHORT_URL_LENGTH; i++) {
            shortUrl.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return shortUrl.toString();
    }

    @Transactional
    public UrlMapping createShortUrl(String originalUrl, String alias, LocalDateTime expiresAt) {
        if (alias != null && !alias.trim().isEmpty()) {
            if (urlMappingRepository.existsByAlias(alias)) {
                throw new IllegalArgumentException("Alias already exists: " + alias);
            }
        }

        String shortUrl;
        if (alias != null && !alias.trim().isEmpty()) {
            shortUrl = alias;
        } else {
            do {
                shortUrl = generateShortUrl();
            } while (urlMappingRepository.existsByShortUrl(shortUrl));
        }

        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl(shortUrl)
                .originalUrl(originalUrl)
                .alias(alias)
                .expiresAt(expiresAt)
                .build();

        log.info("Creating new URL mapping: {} -> {}", shortUrl, originalUrl);
        return urlMappingRepository.save(urlMapping);
    }

    @Transactional(readOnly = true)
    public Optional<UrlMapping> getOriginalUrl(String shortUrl) {
        Optional<UrlMapping> urlMapping = urlMappingRepository
                .findActiveByShortUrlOrAlias(shortUrl, LocalDateTime.now());

        urlMapping.ifPresent(mapping -> {
            mapping.incrementClickCount();
            urlMappingRepository.incrementClickCount(mapping.getId());
            log.debug("Incremented click count for URL: {}", shortUrl);
        });

        return urlMapping;
    }

    public String buildShortUrl(String shortUrl) {
        return baseUrl + "/" + shortUrl;
    }
}
