package url_shortener.url_shortener.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import url_shortener.url_shortener.dto.CreateShortUrlRequest;
import url_shortener.url_shortener.dto.CreateShortUrlResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UrlShortenerIntegrationTest {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createShortUrlAndRedirect_IntegrationTest() {
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl("https://httpbin.org/status/200")
                .alias("integration-test")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        ResponseEntity<CreateShortUrlResponse> createResponse = restTemplate
                .postForEntity("/api/v1/urls/shorten", request, CreateShortUrlResponse.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals("integration-test", createResponse.getBody().getShortUrl());

        ResponseEntity<String> redirectResponse = restTemplate.getForEntity(
                "/integration-test",
                String.class
        );

        assertTrue(redirectResponse.getStatusCode().is3xxRedirection());

        if (redirectResponse.getStatusCode() == HttpStatus.FOUND) {
            assertNotNull(redirectResponse.getHeaders().getLocation());
            assertEquals("https://httpbin.org/status/200",
                    redirectResponse.getHeaders().getLocation().toString());
        }
    }

    @Test
    void createShortUrlAndGetStats_IntegrationTest() {
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl("https://example.com/stats-test")
                .alias("stats-test")
                .build();

        ResponseEntity<CreateShortUrlResponse> createResponse = restTemplate
                .postForEntity("/api/v1/urls/shorten", request, CreateShortUrlResponse.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());

        for (int i = 0; i < 3; i++) {
            restTemplate.getForEntity("/stats-test", String.class);
        }

        ResponseEntity<String> statsResponse = restTemplate
                .getForEntity("/api/v1/urls/stats-test/stats", String.class);

        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        assertTrue(statsResponse.getBody().contains("\"clickCount\":3"));
    }

    @Test
    void createShortUrlWithDuplicateAlias_ShouldReturnError() {
        CreateShortUrlRequest request1 = CreateShortUrlRequest.builder()
                .originalUrl("https://example.com/first")
                .alias("duplicate-alias")
                .build();

        ResponseEntity<CreateShortUrlResponse> response1 = restTemplate
                .postForEntity("/api/v1/urls/shorten", request1, CreateShortUrlResponse.class);

        assertEquals(HttpStatus.OK, response1.getStatusCode());

        CreateShortUrlRequest request2 = CreateShortUrlRequest.builder()
                .originalUrl("https://example.com/second")
                .alias("duplicate-alias")
                .build();

        ResponseEntity<String> response2 = restTemplate
                .postForEntity("/api/v1/urls/shorten", request2, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertTrue(response2.getBody().contains("Alias already exists"));
    }

    @Test
    void getNonExistentUrl_ShouldReturnNotFound() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/v1/urls/nonexistent/stats", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}