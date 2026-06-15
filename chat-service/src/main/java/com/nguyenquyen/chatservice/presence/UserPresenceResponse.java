package com.nguyenquyen.chatservice.presence;

import java.time.Instant;
import java.util.Map;

public record UserPresenceResponse(
        Map<String, UserPresence> presenceData
) {

    public record UserPresence(
            PresenceStatus status,
            Instant lastSeen,
            boolean isOnline
    ) {}
}