package com.nguyenquyen.searchservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.nguyenquyen.searchservice.user.UserIndexService;
import com.nguyenquyen.searchservice.kafka.event.UserEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserIndexService userIndexService;

    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "search-service",
            properties = {"spring.json.value.default.type=com.nguyenquyen.searchservice.kafka.event.UserEvent"}
    )
    public void consume(UserEvent event) {
        log.info("Received user event: type={}, userId={}", event.getType(), event.getUserId());

        try {
            switch (event.getType()) {
                case "USER_CREATED" -> userIndexService.indexUser(event);
                case "USER_UPDATED" -> userIndexService.updateUser(event);
                case "USER_DELETED" -> userIndexService.deleteUser(event.getUserId());

                default -> log.debug("Ignoring user event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing user event: type={}, userId={}",
                    event.getType(), event.getUserId(), e);
        }
    }
}
