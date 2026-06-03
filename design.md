Dưới đây là thiết kế tổng quan cho dự án **Realtime Chatting Platform**

**1. Kiến Trúc Tổng Quan**

```text
Client Web/Mobile
      |
      v
API Gateway
      |
      +-- Auth Service
      +-- User Service
      +-- Friend Service
      +-- Message Service
      +-- Socket Service
      +-- Notification Service
      +-- File Service
      +-- Search Service
      |
      v
Eureka Discovery Server

Infrastructure:
- PostgreSQL: auth, user
- MongoDB: messages, notifications, files
- Redis: token/session cache, presence, typing state
- Kafka: async events
- Elasticsearch: user/message search
```

**Service Chính**

| Service | Nhiệm vụ | DB |
|---|---|---|
| `api-gateway` | Entry point, route, JWT filter, rate limit, fallback | Redis optional |
| `discovery-server` | Eureka service registry | None |
| `auth-service` | Register, login, refresh token, logout, OTP optional | PostgreSQL, Redis |
| `user-service` | Profile, avatar, setting, privacy | PostgreSQL |
| `friend-service` | Friend request, accept, block, contact | PostgreSQL hoặc Neo4j |
| `message-service` | Conversation, message, reaction, attachment metadata | MongoDB |
| `socket-service` | WebSocket, online/offline, typing, realtime delivery | Redis |
| `notification-service` | In-app notification, email optional, unread count | MongoDB |
| `file-service` | Upload file, avatar, attachment, presigned URL optional | MongoDB + local/S3 |
| `search-service` | Search users/messages/conversations | Elasticsearch |

---

**2. Database Design**

Nên dùng **PostgreSQL cho dữ liệu quan hệ**, **MongoDB cho message/notification**, **Redis cho trạng thái realtime**, **Elasticsearch cho search index**.

---

**PostgreSQL - Auth Service**

`accounts`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `email` | varchar | unique |
| `phone` | varchar | unique nullable |
| `password_hash` | varchar | BCrypt |
| `status` | enum | `ACTIVE`, `LOCKED`, `UNVERIFIED`, `DELETED` |
| `email_verified` | boolean | default false |
| `phone_verified` | boolean | default false |
| `last_login_at` | timestamp | nullable |
| `created_at` | timestamp |  |
| `updated_at` | timestamp |  |

`refresh_tokens`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `account_id` | UUID | FK accounts |
| `token_hash` | varchar | không lưu raw token |
| `device_id` | UUID | nullable |
| `expires_at` | timestamp |  |
| `revoked_at` | timestamp | nullable |
| `created_at` | timestamp |  |

`device_sessions`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `account_id` | UUID | FK |
| `device_name` | varchar | Chrome, iPhone... |
| `device_type` | enum | `WEB`, `ANDROID`, `IOS` |
| `ip_address` | varchar |  |
| `user_agent` | text |  |
| `last_active_at` | timestamp |  |
| `created_at` | timestamp |  |

---

**PostgreSQL - User Service**

`users`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK, same as account id hoặc mapping |
| `account_id` | UUID | unique |
| `username` | varchar | unique |
| `display_name` | varchar |  |
| `bio` | text | nullable |
| `avatar_url` | text | nullable |
| `cover_url` | text | nullable |
| `gender` | enum | nullable |
| `date_of_birth` | date | nullable |
| `school` | varchar | optional |
| `major` | varchar | optional |
| `year` | int | optional |
| `status` | enum | `ACTIVE`, `INACTIVE`, `DELETED` |
| `created_at` | timestamp |  |
| `updated_at` | timestamp |  |

`user_settings`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `user_id` | UUID | unique |
| `language` | varchar | `vi`, `en` |
| `theme` | enum | `LIGHT`, `DARK`, `SYSTEM` |
| `profile_visibility` | enum | `PUBLIC`, `FRIENDS`, `PRIVATE` |
| `allow_friend_request` | boolean |  |
| `allow_message_from` | enum | `EVERYONE`, `FRIENDS`, `NONE` |
| `notification_enabled` | boolean |  |
| `created_at` | timestamp |  |
| `updated_at` | timestamp |  |

---

**PostgreSQL - Friend Service**

Nếu muốn đơn giản, dùng PostgreSQL. Nếu muốn nổi bật hơn, dùng Neo4j cho friendship graph. Bản đầu nên dùng PostgreSQL.

`friend_requests`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `sender_id` | UUID | user id |
| `receiver_id` | UUID | user id |
| `status` | enum | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `message` | varchar | optional |
| `created_at` | timestamp |  |
| `responded_at` | timestamp | nullable |

`friendships`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `user_id_1` | UUID | smaller UUID hoặc normalized |
| `user_id_2` | UUID |  |
| `created_at` | timestamp |  |

Unique index:

```sql
unique(user_id_1, user_id_2)
```

`blocked_users`

| Field | Type | Note |
|---|---|---|
| `id` | UUID | PK |
| `blocker_id` | UUID | user id |
| `blocked_id` | UUID | user id |
| `reason` | varchar | optional |
| `created_at` | timestamp |  |

---

**MongoDB - Message Service**

`conversations`

```json
{
  "_id": "ObjectId",
  "type": "DIRECT | GROUP",
  "name": "Group name",
  "avatarUrl": "...",
  "createdBy": "userId",
  "memberIds": ["userId1", "userId2"],
  "adminIds": ["userId1"],
  "lastMessage": {
    "messageId": "...",
    "senderId": "...",
    "content": "...",
    "type": "TEXT",
    "createdAt": "timestamp"
  },
  "settings": {
    "allowMemberInvite": true,
    "muteUntil": null
  },
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

Indexes:

```text
memberIds
updatedAt desc
type
```

`conversation_members`

Có thể nhúng trong `conversations`, nhưng tách riêng sẽ dễ query trạng thái từng user.

```json
{
  "_id": "ObjectId",
  "conversationId": "ObjectId",
  "userId": "UUID",
  "role": "OWNER | ADMIN | MEMBER",
  "nickname": "string",
  "joinedAt": "timestamp",
  "leftAt": null,
  "lastReadMessageId": "ObjectId",
  "mutedUntil": null,
  "pinned": false
}
```

`messages`

```json
{
  "_id": "ObjectId",
  "conversationId": "ObjectId",
  "senderId": "UUID",
  "senderName": "Nguyen Van A",
  "senderAvatar": "...",
  "content": "Hello",
  "type": "TEXT | IMAGE | FILE | SYSTEM | CALL",
  "clientMessageId": "uuid-from-client",
  "replyTo": {
    "messageId": "ObjectId",
    "content": "old message",
    "senderId": "UUID"
  },
  "attachments": [
    {
      "fileId": "ObjectId",
      "url": "...",
      "fileName": "report.pdf",
      "mimeType": "application/pdf",
      "size": 123456
    }
  ],
  "reactions": {
    "like": ["userId1", "userId2"],
    "heart": ["userId3"]
  },
  "status": "NORMAL | REVOKED | DELETED",
  "deletedBy": ["userId"],
  "visibleTo": ["userId1", "userId2"],
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

Indexes:

```text
conversationId + createdAt desc
senderId + createdAt desc
clientMessageId unique sparse
```

---

**MongoDB - Notification Service**

`notifications`

```json
{
  "_id": "ObjectId",
  "recipientId": "UUID",
  "actorId": "UUID",
  "type": "FRIEND_REQUEST | MESSAGE | GROUP_INVITE | SYSTEM",
  "title": "New friend request",
  "body": "Minh sent you a friend request",
  "data": {
    "requestId": "...",
    "conversationId": "..."
  },
  "read": false,
  "createdAt": "timestamp",
  "readAt": null
}
```

Indexes:

```text
recipientId + createdAt desc
recipientId + read
```

`notification_preferences`

```json
{
  "_id": "ObjectId",
  "userId": "UUID",
  "messageEnabled": true,
  "friendRequestEnabled": true,
  "groupInviteEnabled": true,
  "emailEnabled": false,
  "dndStart": "22:00",
  "dndEnd": "07:00"
}
```

---

**MongoDB - File Service**

`files`

```json
{
  "_id": "ObjectId",
  "ownerId": "UUID",
  "originalName": "image.png",
  "storedName": "uuid.png",
  "mimeType": "image/png",
  "size": 234567,
  "url": "...",
  "storageProvider": "LOCAL | S3",
  "bucket": "campusconnect",
  "usageType": "AVATAR | MESSAGE_ATTACHMENT | POST_MEDIA",
  "createdAt": "timestamp"
}
```

---

**Elasticsearch Indexes**

`users_index`

```json
{
  "userId": "UUID",
  "username": "minhnguyen",
  "displayName": "Minh Nguyen",
  "bio": "...",
  "school": "IUH",
  "major": "Software Engineering",
  "avatarUrl": "...",
  "status": "ACTIVE",
  "updatedAt": "timestamp"
}
```

`messages_index`

```json
{
  "messageId": "ObjectId",
  "conversationId": "ObjectId",
  "senderId": "UUID",
  "content": "hello",
  "type": "TEXT",
  "memberIds": ["UUID1", "UUID2"],
  "createdAt": "timestamp"
}
```

Quan trọng: khi search message, phải filter `memberIds` chứa user hiện tại để tránh lộ tin nhắn.

---

**Redis Keys**

| Key | Value | TTL |
|---|---|---|
| `auth:refresh:{tokenId}` | account/session info | refresh token TTL |
| `presence:user:{userId}` | `ONLINE` | short TTL |
| `presence:socket:{sessionId}` | userId | socket TTL |
| `typing:{conversationId}:{userId}` | true | 5-10s |
| `rate-limit:{userId}:{api}` | counter | 1m |
| `unread:{userId}:{conversationId}` | count | optional |

---

**3. Kafka Events**

Nên chuẩn hóa event envelope:

```json
{
  "eventId": "uuid",
  "eventType": "user.created",
  "version": 1,
  "source": "user-service",
  "occurredAt": "2026-06-03T10:00:00Z",
  "correlationId": "request-id",
  "payload": {}
}
```

---

**Auth/User Events**

`account.registered`

Producer: `auth-service`  
Consumer: `user-service`, `notification-service`

```json
{
  "accountId": "UUID",
  "email": "student@iuh.edu.vn",
  "phone": "090...",
  "registeredAt": "timestamp"
}
```

`user.created`

Producer: `user-service`  
Consumer: `search-service`, `message-service`, `friend-service`

```json
{
  "userId": "UUID",
  "accountId": "UUID",
  "username": "minhnguyen",
  "displayName": "Minh Nguyen",
  "avatarUrl": null
}
```

`user.updated`

Producer: `user-service`  
Consumer: `search-service`, `message-service`, `notification-service`

```json
{
  "userId": "UUID",
  "username": "minhnguyen",
  "displayName": "Minh Nguyen",
  "avatarUrl": "...",
  "bio": "...",
  "updatedFields": ["displayName", "avatarUrl"]
}
```

`user.deleted`

Producer: `user-service`  
Consumer: `search-service`, `friend-service`, `message-service`

```json
{
  "userId": "UUID",
  "deletedAt": "timestamp"
}
```

---

**Friend Events**

`friend.request.created`

Producer: `friend-service`  
Consumer: `notification-service`, `socket-service`

```json
{
  "requestId": "UUID",
  "senderId": "UUID",
  "receiverId": "UUID",
  "createdAt": "timestamp"
}
```

`friend.request.accepted`

Producer: `friend-service`  
Consumer: `notification-service`, `message-service`, `search-service`

```json
{
  "requestId": "UUID",
  "userId1": "UUID",
  "userId2": "UUID",
  "acceptedAt": "timestamp"
}
```

`friendship.deleted`

Producer: `friend-service`  
Consumer: `message-service`, `search-service`

```json
{
  "userId1": "UUID",
  "userId2": "UUID",
  "deletedAt": "timestamp"
}
```

`user.blocked`

Producer: `friend-service`  
Consumer: `message-service`, `socket-service`

```json
{
  "blockerId": "UUID",
  "blockedId": "UUID",
  "createdAt": "timestamp"
}
```

---

**Message Events**

`conversation.created`

Producer: `message-service`  
Consumer: `socket-service`, `notification-service`, `search-service`

```json
{
  "conversationId": "ObjectId",
  "type": "DIRECT",
  "memberIds": ["UUID1", "UUID2"],
  "createdBy": "UUID"
}
```

`message.created`

Producer: `message-service`  
Consumer: `socket-service`, `notification-service`, `search-service`

```json
{
  "messageId": "ObjectId",
  "conversationId": "ObjectId",
  "senderId": "UUID",
  "receiverIds": ["UUID2"],
  "content": "Hello",
  "type": "TEXT",
  "createdAt": "timestamp"
}
```

`message.updated`

Producer: `message-service`  
Consumer: `socket-service`, `search-service`

```json
{
  "messageId": "ObjectId",
  "conversationId": "ObjectId",
  "updatedBy": "UUID",
  "content": "edited text",
  "updatedAt": "timestamp"
}
```

`message.deleted`

Producer: `message-service`  
Consumer: `socket-service`, `search-service`

```json
{
  "messageId": "ObjectId",
  "conversationId": "ObjectId",
  "deletedBy": "UUID",
  "deleteType": "FOR_ME | FOR_EVERYONE"
}
```

`message.read`

Producer: `message-service` hoặc `socket-service`  
Consumer: `socket-service`, `notification-service`

```json
{
  "conversationId": "ObjectId",
  "userId": "UUID",
  "lastReadMessageId": "ObjectId",
  "readAt": "timestamp"
}
```

---

**Socket Events**

Kafka events nội bộ:

`socket.user.online`

```json
{
  "userId": "UUID",
  "sessionId": "string",
  "connectedAt": "timestamp"
}
```

`socket.user.offline`

```json
{
  "userId": "UUID",
  "sessionId": "string",
  "disconnectedAt": "timestamp"
}
```

WebSocket destinations:

| Client gửi | Ý nghĩa |
|---|---|
| `/app/chat.send` | gửi message |
| `/app/chat.typing` | typing |
| `/app/chat.read` | mark read |
| `/app/presence.ping` | giữ online |

| Client subscribe | Ý nghĩa |
|---|---|
| `/topic/conversations/{conversationId}` | nhận message trong conversation |
| `/user/queue/messages` | nhận message cá nhân |
| `/user/queue/notifications` | nhận notification |
| `/user/queue/presence` | trạng thái bạn bè |
| `/topic/conversations/{conversationId}/typing` | typing indicator |

---

**Notification Events**

`notification.created`

Producer: `notification-service`  
Consumer: `socket-service`

```json
{
  "notificationId": "ObjectId",
  "recipientId": "UUID",
  "type": "MESSAGE",
  "title": "New message",
  "body": "Minh sent you a message",
  "data": {
    "conversationId": "..."
  }
}
```

`notification.read`

Producer: `notification-service`  
Consumer: `socket-service`

```json
{
  "notificationId": "ObjectId",
  "recipientId": "UUID",
  "readAt": "timestamp"
}
```

---

**Search Events**

`user.search.index.requested`

Producer: `user-service`  
Consumer: `search-service`

```json
{
  "userId": "UUID",
  "operation": "CREATE | UPDATE | DELETE"
}
```

`message.search.index.requested`

Producer: `message-service`  
Consumer: `search-service`

```json
{
  "messageId": "ObjectId",
  "conversationId": "ObjectId",
  "operation": "CREATE | UPDATE | DELETE"
}
```

---

**4. API Design Tổng Quan**

Auth:

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout
GET  /auth/me
GET  /auth/devices
DELETE /auth/devices/{id}
```

User:

```text
GET  /users/me
PUT  /users/me
GET  /users/{id}
PUT  /users/me/avatar
GET  /users/settings
PUT  /users/settings
```

Friend:

```text
POST /friend-requests
GET  /friend-requests/incoming
GET  /friend-requests/outgoing
POST /friend-requests/{id}/accept
POST /friend-requests/{id}/reject
DELETE /friendships/{userId}
POST /blocks/{userId}
DELETE /blocks/{userId}
GET /friends
```

Message:

```text
POST /conversations/direct
POST /conversations/group
GET  /conversations
GET  /conversations/{id}
POST /conversations/{id}/members
DELETE /conversations/{id}/members/{userId}
GET  /conversations/{id}/messages
POST /conversations/{id}/messages
PUT  /messages/{id}
DELETE /messages/{id}
POST /messages/{id}/reactions
DELETE /messages/{id}/reactions
POST /conversations/{id}/read
```

Notification:

```text
GET /notifications
POST /notifications/{id}/read
POST /notifications/read-all
GET /notifications/unread-count
```

File:

```text
POST /files/upload
GET  /files/{id}
DELETE /files/{id}
```

Search:

```text
GET /search/users?q=
GET /search/messages?q=&conversationId=
GET /search/conversations?q=
```

---

**5. Luồng Nghiệp Vụ Chính**

**Đăng ký user**

```text
Client -> Gateway -> Auth Service
Auth Service tạo account
Auth Service publish account.registered
User Service consume event và tạo user profile mặc định
User Service publish user.created
Search Service index user
Notification Service tạo welcome notification
```

**Gửi lời mời kết bạn**

```text
Client -> Friend Service
Friend Service tạo friend_request
Friend Service publish friend.request.created
Notification Service tạo notification
Socket Service push realtime notification cho receiver
```

**Accept friend request**

```text
Client -> Friend Service
Friend Service update request ACCEPTED
Friend Service tạo friendship
Friend Service publish friend.request.accepted
Notification Service notify sender
Message Service có thể tạo direct conversation nếu chưa có
Search Service cập nhật relationship score/context
```

**Gửi tin nhắn**

```text
Client -> WebSocket /app/chat.send
Socket Service validate token
Socket Service gọi Message Service hoặc publish command
Message Service lưu message vào MongoDB
Message Service publish message.created
Socket Service consume message.created và push tới member online
Notification Service consume message.created và tạo notification cho member offline
Search Service consume message.created và index nội dung
```

---

**6. Gợi Ý Scope Làm Thực Tế**

Phiên bản đầu nên làm 7 module:

```text
common
api-gateway
discovery-server
auth-service
user-service
friend-service
message-service
socket-service
```

Sau khi ổn mới thêm:

```text
notification-service
file-service
search-service
```

Đây là scope đủ mạnh cho CV nhưng không quá nặng như repo gốc.