package com.nguyenquyen.common.kafka.event;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationEvent {
    private UUID messageId;
    private UUID chatId;
    private String senderId;
    private String label;
    private double confidence;
    private String action;
    
    @Builder.Default
    @JsonDeserialize(using = LenientInstantDeserializer.class)
    private Instant timestamp = Instant.now();

    public static class LenientInstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return Instant.now();
            }
            try {
                return Instant.parse(text);
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
                } catch (Exception ex) {
                    return Instant.now();
                }
            }
        }
    }
}
