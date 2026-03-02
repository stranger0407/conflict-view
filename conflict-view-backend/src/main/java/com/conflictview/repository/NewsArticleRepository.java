package com.conflictview.repository;

import com.conflictview.model.NewsArticle;
import com.conflictview.model.enums.SentimentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    Optional<NewsArticle> findByUrl(String url);

    boolean existsByUrl(String url);

    Page<NewsArticle> findByConflictIdOrderByPublishedAtDesc(UUID conflictId, Pageable pageable);

    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.conflict.id = :conflictId
            AND (:sentiment IS NULL OR a.sentiment = :sentiment)
            AND (:sourceDomain IS NULL OR a.sourceDomain = :sourceDomain)
            ORDER BY a.publishedAt DESC
            """)
    Page<NewsArticle> findByConflictFiltered(
            @Param("conflictId") UUID conflictId,
            @Param("sentiment") SentimentType sentiment,
            @Param("sourceDomain") String sourceDomain,
            Pageable pageable
    );

    @Query("""
            SELECT a.sourceDomain, COUNT(a) as cnt
            FROM NewsArticle a
            WHERE a.conflict.id = :conflictId
            GROUP BY a.sourceDomain
            ORDER BY cnt DESC
            """)
    List<Object[]> countBySourceForConflict(@Param("conflictId") UUID conflictId);

    long countByConflictId(UUID conflictId);

    @Query("""
            SELECT a.conflict.id, COUNT(a)
            FROM NewsArticle a
            WHERE a.conflict.id IN :ids
            GROUP BY a.conflict.id
            """)
    List<Object[]> countByConflictIds(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT CAST(a.sentiment AS string), COUNT(a) as cnt
            FROM NewsArticle a
            WHERE a.conflict.id = :conflictId
            GROUP BY a.sentiment
            """)
    List<Object[]> countBySentimentForConflict(@Param("conflictId") UUID conflictId);

    @Query("""
            SELECT COALESCE(AVG(a.reliabilityScore), 0)
            FROM NewsArticle a
            WHERE a.conflict.id = :conflictId AND a.reliabilityScore IS NOT NULL
            """)
    Double averageReliabilityForConflict(@Param("conflictId") UUID conflictId);

    @Query("""
            SELECT EXTRACT(YEAR FROM a.publishedAt) as yr,
                   EXTRACT(MONTH FROM a.publishedAt) as mo,
                   COUNT(a) as cnt
            FROM NewsArticle a
            WHERE a.conflict.id = :conflictId AND a.publishedAt IS NOT NULL
            GROUP BY yr, mo
            ORDER BY yr ASC, mo ASC
            """)
    List<Object[]> monthlyArticleCountForConflict(@Param("conflictId") UUID conflictId);

    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.conflict.id = :conflictId
            ORDER BY a.publishedAt DESC
            """)
    List<NewsArticle> findByConflictIdOrderByPublishedAtDesc(@Param("conflictId") UUID conflictId);
}
