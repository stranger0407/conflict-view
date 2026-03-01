package com.conflictview.repository;

import com.conflictview.model.Conflict;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ConflictType;
import com.conflictview.model.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConflictRepository extends JpaRepository<Conflict, UUID> {

    List<Conflict> findByStatusOrderBySeverityDescNameAsc(ConflictStatus status);

    List<Conflict> findByStatus(ConflictStatus status);

    @Query("""
            SELECT c FROM Conflict c
            WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.region) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:region IS NULL OR LOWER(c.region) = LOWER(:region))
            AND (:severity IS NULL OR c.severity = :severity)
            AND (:type IS NULL OR c.conflictType = :type)
            AND (:status IS NULL OR c.status = :status)
            ORDER BY c.severity DESC, c.updatedAt DESC
            """)
    List<Conflict> search(
            @Param("q") String q,
            @Param("region") String region,
            @Param("severity") Severity severity,
            @Param("type") ConflictType type,
            @Param("status") ConflictStatus status
    );

    long countByStatus(ConflictStatus status);

    @Query("SELECT COUNT(c) FROM Conflict c WHERE c.severity = :severity AND c.status = 'ACTIVE'")
    long countActiveBySeverity(@Param("severity") Severity severity);
}
