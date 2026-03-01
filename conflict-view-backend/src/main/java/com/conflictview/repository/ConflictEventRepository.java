package com.conflictview.repository;

import com.conflictview.model.ConflictEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConflictEventRepository extends JpaRepository<ConflictEvent, UUID> {

    List<ConflictEvent> findByConflictIdOrderByEventDateDesc(UUID conflictId);

    @Query("""
            SELECT e.eventType, COUNT(e) as cnt
            FROM ConflictEvent e
            WHERE e.conflict.id = :conflictId
            GROUP BY e.eventType
            ORDER BY cnt DESC
            """)
    List<Object[]> countByEventType(@Param("conflictId") UUID conflictId);

    @Query("""
            SELECT EXTRACT(YEAR FROM e.eventDate) as yr,
                   EXTRACT(MONTH FROM e.eventDate) as mo,
                   COUNT(e) as cnt,
                   COALESCE(SUM(e.fatalitiesReported), 0) as casualties
            FROM ConflictEvent e
            WHERE e.conflict.id = :conflictId
            GROUP BY yr, mo
            ORDER BY yr DESC, mo DESC
            """)
    List<Object[]> monthlyStatsForConflict(@Param("conflictId") UUID conflictId);
}
