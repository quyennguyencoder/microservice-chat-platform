package com.nguyenquyen.notificationservice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.nguyenquyen.notificationservice.kafka.event.BaseEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void createAndSendNotification(BaseEvent event);

    Page<NotificationResponse> getMyNotifications(Pageable pageable);

    Page<NotificationResponse> getMyUnreadNotifications(Pageable pageable);

    long getUnreadCount();

    int markAllAsRead();

    int markAsRead(List<UUID> ids);

    void delete(UUID id);
}
