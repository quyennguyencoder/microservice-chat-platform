package com.nguyenquyen.userservice.repository;

import com.nguyenquyen.userservice.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Search users by displayName, username, or email.
     * Case-insensitive LIKE search.
     *
     * CONCEPT: JPQL vs Native SQL
     * ────────────────────────────
     * JPQL (Java Persistence Query Language) uses entity/field names,
     * not table/column names. Portable across databases.
     *
     * LOWER() → case-insensitive search
     * %:query% → contains query anywhere in string
     *
     * Future: Replace with Elasticsearch for full-text search with ranking.
     */
    @Query("""
        SELECT p FROM UserProfile p
        WHERE LOWER(p.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.username)    LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.email)       LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY p.displayName ASC
        """)
    Page<UserProfile> searchByQuery(@Param("query") String query, Pageable pageable);
}
