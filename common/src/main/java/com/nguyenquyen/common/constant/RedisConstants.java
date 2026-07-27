package com.nguyenquyen.common.constant;

public final class RedisConstants {

    private RedisConstants() {
        // Prevent instantiation
    }

    // ===== Pub/Sub Channels (Used in chat-service and notification-service) =====
    public static final String CHAT_CHANNEL = "ws-chat-channel";
    public static final String PRESENCE_CHANNEL = "ws-presence-channel";

    // ===== Cache Keys (Used in @Cacheable, @CacheEvict across services) =====
    public static final String MY_CHATS_CACHE = "myChats";
    public static final String MY_FRIENDS_CACHE = "myFriends";

    // ===== Key Prefixes (Used with RedisTemplate operations) =====
    public static final String PRESENCE_KEY_PREFIX = "presence:user:";
    public static final String LAST_SEEN_KEY_PREFIX = "lastseen:user:";
    public static final String ACTIVE_CHAT_KEY_PREFIX = "active-chat:user:";
    public static final String TYPING_KEY_PREFIX = "typing:chat:";
    
    public static final String UNREAD_COUNT_KEY_PREFIX = "notification:unread:";
}
