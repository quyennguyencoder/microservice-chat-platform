package com.nguyenquyen.conversationservice.repository;

import com.nguyenquyen.conversationservice.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    /** Returns all groups where the given user is a member (any role), newest first. */
    @Query("SELECT g FROM Group g JOIN g.members m ON m.userId = :userId ORDER BY g.updatedAt DESC")
    Page<Group> findAllByMemberUserId(@Param("userId") UUID userId, Pageable pageable);
}
