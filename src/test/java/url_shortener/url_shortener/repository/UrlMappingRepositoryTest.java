package url_shortener.url_shortener.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import url_shortener.url_shortener.entity.UrlMapping;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UrlMappingRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Test
    void findByShortUrl_WithExistingShortUrl_ShouldReturnUrlMapping() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl("abc123")
                .originalUrl("https://example.com")
                .build();

        entityManager.persistAndFlush(urlMapping);

        Optional<UrlMapping> found = urlMappingRepository.findByShortUrl("abc123");

        assertTrue(found.isPresent());
        assertEquals("abc123", found.get().getShortUrl());
        assertEquals("https://example.com", found.get().getOriginalUrl());
    }

    @Test
    void findActiveByShortUrlOrAlias_WithValidUrl_ShouldReturnUrlMapping() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl("valid123")
                .originalUrl("https://valid.com")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        entityManager.persistAndFlush(urlMapping);

        Optional<UrlMapping> found = urlMappingRepository
                .findActiveByShortUrlOrAlias("valid123", LocalDateTime.now());

        assertTrue(found.isPresent());
        assertEquals("valid123", found.get().getShortUrl());
    }

    @Test
    void findActiveByShortUrlOrAlias_WithExpiredUrl_ShouldReturnEmpty() {
        UrlMapping expiredMapping = UrlMapping.builder()
                .shortUrl("expired123")
                .originalUrl("https://expired.com")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        entityManager.persistAndFlush(expiredMapping);

        Optional<UrlMapping> found = urlMappingRepository
                .findActiveByShortUrlOrAlias("expired123", LocalDateTime.now());

        assertFalse(found.isPresent());
    }

    @Test
    void existsByAlias_WithExistingAlias_ShouldReturnTrue() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl("short456")
                .originalUrl("https://alias.com")
                .alias("my-alias")
                .build();

        entityManager.persistAndFlush(urlMapping);

        boolean exists = urlMappingRepository.existsByAlias("my-alias");

        assertTrue(exists);
    }

    @Test
    void incrementClickCount_ShouldUpdateCount() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl("clicktest")
                .originalUrl("https://click.com")
                .clickCount(5L)
                .build();

        UrlMapping saved = entityManager.persistAndFlush(urlMapping);

        urlMappingRepository.incrementClickCount(saved.getId());
        entityManager.flush();
        entityManager.clear();

        UrlMapping updated = entityManager.find(UrlMapping.class, saved.getId());
        assertEquals(6L, updated.getClickCount());
    }

    @Test
    void findByAlias_WithExistingAlias_ShouldReturnUrlMapping() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortUrl("aliasShort")
                .originalUrl("https://alias-test.com")
                .alias("test-alias")
                .build();

        entityManager.persistAndFlush(urlMapping);

        Optional<UrlMapping> found = urlMappingRepository.findByAlias("test-alias");

        assertTrue(found.isPresent());
        assertEquals("test-alias", found.get().getAlias());
        assertEquals("aliasShort", found.get().getShortUrl());
    }
}