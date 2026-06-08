package com.nguyenquyen.userservice.enums;

/**
 * User Online Status
 *
 * CONCEPT: Online Status in Chat Apps
 * ─────────────────────────────────────
 * ONLINE  → User is actively connected (WebSocket open)
 * OFFLINE → User is disconnected (no active session)
 * AWAY    → User set themselves as away (e.g. "away from keyboard")
 * BUSY    → User set themselves as busy (e.g. "do not disturb")
 *
 * Future: WebSocket service will update this in real-time.
 * For now: updated via REST API PUT /api/users/me/status.
 *
 * Privacy: Users can choose who sees their online status
 * (everyone, contacts only, nobody) — handled via privacyLevel field.
 */
public enum OnlineStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY
}
