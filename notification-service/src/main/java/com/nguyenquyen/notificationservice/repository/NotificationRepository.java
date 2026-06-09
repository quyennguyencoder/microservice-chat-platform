package com.nguyenquyen.notificationservice.repository;

import com.nguyenquyen.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns paginated notifications for a user, newest first.
     *
     * @param userId   the user's UUID
     * @param pageable pagination + sort configuration
     * @return page of notifications
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Counts unread notifications for a user.
     *
     * @param userId the user's UUID
     * @return count of unread notifications
     */
    long countByUserIdAndReadFalse(UUID userId);

    /**
     * Marks all unread notifications for a user as read in a single UPDATE.
     *
     * @param userId the user's UUID
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsReadForUser(@Param("userId") UUID userId);
}
