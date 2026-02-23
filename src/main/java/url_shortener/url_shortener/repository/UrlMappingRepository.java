package url_shortener.url_shortener.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import url_shortener.url_shortener.entity.UrlMapping;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortUrl(String shortUrl);

    Optional<UrlMapping> findByAlias(String alias);

    boolean existsByShortUrl(String shortUrl);

    boolean existsByAlias(String alias);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Query("SELECT u FROM UrlMapping u WHERE (u.shortUrl = :key OR u.alias = :key) " +
            "AND (u.expiresAt IS NULL OR u.expiresAt > :now)")
    Optional<UrlMapping> findActiveByShortUrlOrAlias(@Param("key") String key, @Param("now") LocalDateTime now);
}
