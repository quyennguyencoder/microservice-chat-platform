# Redis Deep-Dive: Quản lý Trạng thái (Presence) & Đếm thông báo (Unread Counter)

Tài liệu này mô tả kiến trúc, lý do lựa chọn, và chi tiết triển khai **Redis In-Memory Data Grid** trong hệ thống phân tán ZChat Microservices.

---

## 1. Tại sao ZChat sử dụng Redis thay vì Cơ sở dữ liệu quan hệ (PostgreSQL)?

Trong một hệ thống nhắn tin thời gian thực với hàng nghìn hoặc hàng triệu người dùng đồng thời, có hai nghiệp vụ xảy ra với **tần suất cực kì cao (High-Throughput / High-Frequency)** mỗi giây:
1. **Cập nhật Trạng thái Online / Typing (`Presence Management`)**: Người dùng mở tab, gõ phím, tắt tab, chuyển phòng chat.
2. **Tăng/Giảm bộ đếm thông báo chưa đọc (`Unread Notification Counter`)**: Khi có tin nhắn mới tới, số badge đỏ trên quả chuông hay phòng chat tăng lên `+1`.

### So sánh PostgreSQL vs Redis:
* **Nếu dùng PostgreSQL:**
  * Mỗi cú nhấp chuột hay nhịp gõ phím phải thực hiện câu lệnh `UPDATE users SET last_seen = NOW() WHERE id = ...`.
  * Khóa bản ghi (Row Lock / Table Lock) xảy ra liên tục, làm cạn kiệt bộ kết nối (Connection Pool Exhaustion).
  * Ổ cứng phải ghi chép nhật ký I/O (WAL disk I/O) liên tục cho các dữ liệu mang tính chất tạm thời, khiến hệ thống chậm chạp và dễ sập.
* **Sử dụng Redis (`In-Memory Data Grid`):**
  * Toàn bộ dữ liệu nằm trong **RAM**, cho phép tốc độ Đọc/Ghi cực nhanh (Độ trễ dưới **1 mili-giây** - `< 1ms`).
  * Hỗ trợ tự động xóa dữ liệu khi hết hạn thông qua cơ chế **TTL (Time-To-Live)** — lý tưởng để tự động ngắt trạng thái Online hoặc Đang gõ phím khi người dùng ngắt kết nối đột ngột mà không cần chạy job dọn dẹp trong DB.
  * Hỗ trợ các phép toán nguyên tử (`Atomic Increment / Decrement`) giúp đếm thông báo không bao giờ bị sai lệch khi có nhiều luồng truy cập đồng thời.

---

## 2. Chi tiết Triển khai trong Codebase ZChat

### A. Redis Pub/Sub: Đồng bộ tin nhắn liên Server (Distributed WebSocket)
Đây là tính năng cực kỳ quan trọng cho phép `chat-service` có thể Scale-Out ra nhiều máy chủ.
* **Vấn đề:** User A nối cáp WebSocket vào Server 1. User B nối vào Server 2. Làm sao Server 1 gửi tin nhắn cho Server 2?
* **Giải pháp ZChat:**
  * Không gửi trực tiếp. Thay vào đó, Server 1 sử dụng `RedisMessagePublisher` phát (`Publish`) gói tin vào kênh **Redis Channel `ws-chat-channel`**.
  * Server 2 chạy một `RedisMessageSubscriber` luôn lắng nghe (`Subscribe`) kênh này. Khi Server 2 nhận được thông báo, nó mở RAM nội bộ ra, phát hiện User B đang cắm cáp tại máy mình $\rightarrow$ Server 2 trực tiếp đẩy tin nhắn vào ống WebSocket của User B.
  * Tương tự với kênh **`ws-presence-channel`** dùng để báo hiệu (Broadcast) cho toàn bộ các server biết mỗi khi có một ai đó vừa Online/Offline.

### B. Quản lý Trạng thái Người dùng (`PresenceService.java` - `chat-service`)
Vị trí file: `chat-service/src/main/java/com/nguyenquyen/chatservice/presence/PresenceService.java`

| Key Prefix Redis | Cấu trúc Dữ liệu | TTL | Mục đích & Chi tiết nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `presence:user:{userId}` | **String** (Epoch Milliseconds) | `60s` (Cấu hình: `presence.online-threshold-seconds`) | **Xác định trạng thái Online:** Khi user đang mở app, định kỳ mỗi 30 - 45s gửi WebSocket `HEARTBEAT`, server cập nhật lại timestamp hiện tại và gia hạn TTL lên 60s. Nếu rớt mạng hoặc tắt app, sau 60s key tự hủy $\rightarrow$ User chuyển sang Offline. |
| `lastseen:user:{userId}` | **String** (Epoch Milliseconds) | `30 ngày` | **Thời điểm truy cập cuối cùng:** Được cập nhật mỗi khi user có tương tác hoặc khi ngắt kết nối (`removePresence`). Dùng để tính toán chuỗi hiển thị thân thiện như: *"Vừa mới truy cập"*, *"Hôm qua"*, *"Cách đây 5 phút"* (`calculateStatus`). |
| `active-chat:user:{userId}` | **String** (`UUID chatId`) | Không TTL (Xóa khi rời phòng/disconnect) | **Phòng chat đang mở:** Biết chính xác user đang đứng ở phòng chat nào (`setActiveChat`). Nếu user đang mở phòng A, khi có tin nhắn mới tới phòng A, hệ thống không cần gửi thông báo đẩy (Push Notification) gây phiền hà. |
| `typing:chat:{chatId}` | **Set** (`userId`) | `6s` (Cấu hình: `presence.typing-timeout-seconds`) | **Hiệu ứng "Đang gõ phím..." (`Typing Indicator`):** Khi User A gõ tin nhắn, ID của User A được thêm (`opsForSet().add`) vào tập hợp Set của phòng chat cùng TTL `6s`. Vì dùng Set, dù gõ 100 phím cũng không bị lặp ID. Sau 6s ngừng gõ, Redis xóa key $\rightarrow$ Giao diện tự động tắt hiệu ứng gõ phím. |

#### Code Snippet tiêu biểu (`PresenceService.java`):
```java
public void updatePresence(String userId) {
    Instant now = Instant.now();

    // 1. Cập nhật key Online với TTL 60 giây
    String presenceKey = PRESENCE_KEY_PREFIX + userId;
    redisTemplate.opsForValue().set(presenceKey, now.toEpochMilli());
    redisTemplate.expire(presenceKey, Duration.ofSeconds(onlineThresholdSeconds)); // 60s

    // 2. Cập nhật Last Seen với TTL 30 ngày
    String lastSeenKey = LAST_SEEN_KEY_PREFIX + userId;
    redisTemplate.opsForValue().set(lastSeenKey, now.toEpochMilli());
    redisTemplate.expire(lastSeenKey, Duration.ofDays(30));
}

public void startTyping(String userId, UUID chatId) {
    String key = TYPING_KEY_PREFIX + chatId;
    redisTemplate.opsForSet().add(key, userId);
    redisTemplate.expire(key, Duration.ofSeconds(typingTimeoutSeconds)); // 6s TTL
}
```

---

### B. Quản lý Bộ đếm Thông báo chưa đọc (`UnreadCounterService.java` - `notification-service`)
Vị trí file: `notification-service/src/main/java/com/nguyenquyen/notificationservice/counter/UnreadCounterService.java`

* **Key Redis:** `notification:unread:{userId}`
* **Cấu trúc:** **String (Atomic Long Counter)**
* **Cơ chế hoạt động:**
  * **Tăng bộ đếm (`increment`):** Dùng lệnh nguyên tử `redisTemplate.opsForValue().increment(key)` (tương đương lệnh `INCR` trong Redis thuần). Đảm bảo 100% không bị Race Condition khi có 10 tin nhắn tới cùng 1 mili-giây.
  * **Giảm bộ đếm (`decrement`):** Khi người dùng đọc một số thông báo nhất định, giảm số lượng tương ứng nhưng không bao giờ cho phép âm (`Math.max(0, current - count)`).
  * **Xóa / Đọc hết (`reset`):** Khi người dùng bấm "Đọc tất cả", set giá trị về `0L`.

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nếu Redis bị mất điện hoặc khởi động lại, hệ thống có bị mất dữ liệu trạng thái Online hay thông báo không?"*
**Cấu trúc trả lời:**
> *"Dạ, với dữ liệu **Trạng thái Online (`Presence`) và Typing**, việc mất đi khi Redis restart là **hoàn toàn chấp nhận được và đúng bản chất**, vì khi các Client kết nối lại WebSocket sau vài giây, cơ chế `HEARTBEAT` sẽ tự động đăng ký lại trạng thái Online lên Redis mới.
> Còn đối với **Bộ đếm thông báo (`Unread Counter`)** hoặc dữ liệu cần bền vững, trong cấu hình Redis cho môi trường Production, chúng ta bật cơ chế lưu trữ bền vững **AOF (Append Only File) mỗi giây (`appendfsync everysec`)** kết hợp **RDB Snapshot**. Đồng thời, số lượng thông báo thực tế vẫn được lưu gốc trong bảng `notifications` của PostgreSQL, Redis đóng vai trò là bộ đếm siêu tốc để phục vụ giao diện."*

### ❓ Câu hỏi 2: *"Tại sao khi User A gõ phím, em lại đặt TTL cho key `typing:chat:{chatId}` là 6 giây mà không phải là xóa thủ công khi họ dừng gõ?"*
**Cấu trúc trả lời:**
> *"Dạ, nếu chỉ dựa vào sự kiện `TYPING_STOP` từ Client gửi lên để xóa thủ công, sẽ xảy ra tình trạng **Kẹt hiệu ứng gõ phím ảo (`Zombie Typing State`)** nếu User A bị rớt Wifi, tắt ngang trình duyệt, hoặc bị crash app ngay giữa lúc đang gõ.
> Việc đặt **Redis TTL 6 giây** là một chốt chặn an toàn (`Safety Guard`): Nếu User A tiếp tục gõ, Frontend sẽ định kỳ gửi `TYPING_START` (được throttle 2.5s/lần) để gia hạn TTL lên 6s. Nếu họ ngừng gõ hoặc rớt mạng đột ngột, đúng 6 giây sau Redis tự động xóa ID khỏi Set, giúp giao diện người nhận luôn chính xác tuyệt đối."*
