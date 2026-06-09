package com.nguyenquyen.conversationservice.repository;

import com.nguyenquyen.conversationservice.entity.GroupMember;
import com.nguyenquyen.conversationservice.entity.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserIdAndRole(UUID groupId, UUID userId, GroupRole role);

    /** JPQL delete — avoids CascadeType.ALL re-inserting from parent Group in memory. */
    @Transactional
    @Modifying
    @Query("DELETE FROM GroupMember m WHERE m.group.id = :groupId AND m.userId = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
