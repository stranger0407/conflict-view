package com.conflictview.repository;

import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OsintResourceRepository extends JpaRepository<OsintResource, UUID> {

    boolean existsByUrl(String url);

    Page<OsintResource> findByConflictIdOrderByPublishedAtDesc(UUID conflictId, Pageable pageable);

    Page<OsintResource> findByConflictIdAndResourceTypeOrderByPublishedAtDesc(
            UUID conflictId, ResourceType resourceType, Pageable pageable);

    @Query("""
            SELECT CAST(o.resourceType AS string), COUNT(o)
            FROM OsintResource o
            WHERE o.conflict.id = :conflictId
            GROUP BY o.resourceType
            """)
    List<Object[]> countByConflictIdGroupByResourceType(@Param("conflictId") UUID conflictId);
}
