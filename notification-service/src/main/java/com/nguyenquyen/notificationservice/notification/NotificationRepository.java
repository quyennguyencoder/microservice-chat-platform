package com.nguyenquyen.notificationservice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByUpdatedAtDesc(
            String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByUpdatedAtDesc(
            String recipientId, NotificationStatus status, Pageable pageable);

    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = 'NEW_MESSAGE' 
        AND n.referenceId = :chatId 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadMessagesNotification(
            @Param("recipientId") String recipientId,
            @Param("chatId") UUID chatId
    );



    @Modifying
    @Query("""
        UPDATE Notification n 
        SET n.status = :newStatus, n.readAt = :readAt 
        WHERE n.recipientId = :recipientId AND n.status = :currentStatus
    """)
    int markAllAsRead(
            @Param("recipientId") String recipientId,
            @Param("currentStatus") NotificationStatus currentStatus,
            @Param("newStatus") NotificationStatus newStatus,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query("""
        UPDATE Notification n 
        SET n.status = :newStatus, n.readAt = :readAt 
        WHERE n.id IN :ids AND n.recipientId = :recipientId
    """)
    int markAsRead(
            @Param("ids") List<UUID> ids,
            @Param("recipientId") String recipientId,
            @Param("newStatus") NotificationStatus newStatus,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);
}