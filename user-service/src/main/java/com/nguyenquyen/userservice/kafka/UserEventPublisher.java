package com.nguyenquyen.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.nguyenquyen.common.kafka.event.UserEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    public void publish(UserEvent event) {
        String key = event.getRecipientId();

        kafkaTemplate.send(userEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user event: {}", event, ex);
                    } else {
                        log.debug("User event published: type={}, recipient={}",
                                event.getType(), event.getRecipientId());
                    }
                });
    }
}