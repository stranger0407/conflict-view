package com.conflictview.repository;

import com.conflictview.model.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, UUID> {

    Optional<NewsSource> findByDomain(String domain);

    boolean existsByDomain(String domain);
}
