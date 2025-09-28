package url_shortener.url_shortener.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import url_shortener.url_shortener.entity.UrlMapping;
import url_shortener.url_shortener.service.UrlShortenerService;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    @GetMapping("/{shortUrl}")
    public void redirectToOriginalUrl(
            @PathVariable String shortUrl,
            HttpServletResponse response) throws IOException {

        log.info("Redirect request for short URL: {}", shortUrl);

        UrlMapping urlMapping = urlShortenerService.getOriginalUrl(shortUrl)
                .orElseThrow(() -> new RuntimeException("URL not found: " + shortUrl));

        if (urlMapping.isExpired()) {
            log.warn("Short URL has expired: {}", shortUrl);
            response.sendError(HttpStatus.GONE.value(), "URL has expired");
            return;
        }

        log.info("Redirecting {} to {}", shortUrl, urlMapping.getOriginalUrl());
        response.sendRedirect(urlMapping.getOriginalUrl());
    }
}
