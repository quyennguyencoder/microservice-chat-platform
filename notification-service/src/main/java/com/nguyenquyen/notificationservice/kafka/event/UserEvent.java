package com.nguyenquyen.notificationservice.kafka.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEvent extends BaseEvent {

    private String visitorId;

    @Override
    public UUID getReferenceId() {
        return null;
    }
}