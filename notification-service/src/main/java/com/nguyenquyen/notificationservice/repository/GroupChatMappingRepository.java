package com.nguyenquyen.notificationservice.repository;

import com.nguyenquyen.notificationservice.entity.GroupChatMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupChatMappingRepository extends JpaRepository<GroupChatMapping, UUID> {

    /**
     * Looks up the chatId associated with a group.
     *
     * @param groupId the group's UUID
     * @return an optional containing the mapping if found
     */
    Optional<GroupChatMapping> findByGroupId(UUID groupId);

    /**
     * Checks if a group → chat mapping already exists.
     *
     * @param groupId the group's UUID
     * @return {@code true} if the mapping exists
     */
    boolean existsByGroupId(UUID groupId);
}
