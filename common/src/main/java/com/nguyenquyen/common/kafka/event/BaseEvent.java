package com.nguyenquyen.common.kafka.event;

public interface BaseEvent {
    String getType();
    String getActorId();
    String getRecipientId();
    String getPreviewText();
    String getPreviewImageId();
}
