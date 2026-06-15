package com.nguyenquyen.notificationservice.kafka.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatEvent extends BaseEvent {

    private UUID chatId;
    private UUID messageId;
    private String messageType;

    @Override
    public UUID getReferenceId() {
        return chatId;
    }
}