# Apache Kafka Deep-Dive: Kiến trúc Sự kiện Phân tán (Event-Driven Architecture)

Tài liệu này mô tả chi tiết cách thức **Apache Kafka** được thiết kế, cấu hình, và vận hành như một trục xương sống truyền dữ liệu bất đồng bộ (`Event-Driven Backbone`) giữa các Microservices trong ZChat.

---

## 1. Tại sao ZChat sử dụng Kafka thay vì REST API gọi chéo trực tiếp hay RabbitMQ?

### A. Vấn đề của việc gọi chéo REST API trực tiếp (Synchronous REST Calling):
Giả sử khi người dùng cập nhật ảnh đại diện mới tại `user-service`, nếu dùng REST API gọi tuần tự sang `chat-service` (cập nhật avatar trong group chat), `search-service` (cập nhật index tìm kiếm Elasticsearch), và `storage-service` (đánh dấu ảnh cũ cần xóa):
* **Độ trễ cộng dồn (`High Latency`):** Thời gian phản hồi cho Client = $T_{\text{User}} + T_{\text{Chat}} + T_{\text{Search}} + T_{\text{Storage}}$. User phải đợi rất lâu mới thấy Profile được cập nhật.
* **Hiệu ứng Domino (`Cascading Failure`):** Chỉ cần `search-service` bị chậm hoặc bảo trì, toàn bộ API cập nhật thông tin cá nhân tại `user-service` sẽ bị lỗi 500!
* **Khó mở rộng (`Tight Coupling`):** Sau này nếu có thêm `analytics-service` muốn biết khi nào user cập nhật profile, ta lại phải sửa code của `user-service` để thêm một cú gọi HTTP mới.

### B. So sánh Kafka vs RabbitMQ:
* **RabbitMQ (Smart Broker, Dumb Consumer):** Phù hợp với các task xử lý công việc ngắn hạn (`Work Queue`), tin nhắn sau khi Consumer đọc xong sẽ bị xóa khỏi Broker.
* **Apache Kafka (Dumb Broker, Smart Consumer - Event Log):**
  * Hoạt động như một nhật ký sự kiện bất biến (`Distributed Commit Log`).
  * **Đa người đọc (`Multi-Consumer / Fan-out`):** Khi `user-service` bắn 1 sự kiện `USER_UPDATED` vào Kafka Topic, cả `chat-service`, `search-service`, và `storage-service` đều có thể cùng đọc sự kiện đó độc lập theo tốc độ riêng của mình mà không làm ảnh hưởng đến ai.
  * **Khả năng lưu trữ (`Message Retention`):** Kafka giữ lại sự kiện trên đĩa cứng (7 ngày hoặc tùy chỉnh). Nếu `search-service` bị sập 2 tiếng, sau khi khởi động lại, nó có thể đọc tiếp (`Seek Offset`) đúng từ điểm bị ngắt để đồng bộ lại toàn bộ dữ liệu mà không mất bất kỳ sự kiện nào!

---

## 2. Chi tiết Triển khai trong Codebase ZChat

### A. Định nghĩa Sự kiện Chung tại Module `common`
Vị trí folder: `common/src/main/java/com/nguyenquyen/common/kafka/event/`

Để đảm bảo tính đồng nhất (Schema Contract), toàn bộ các DTO sự kiện được định nghĩa tập trung tại module `common` và đóng gói thành thư viện dùng chung cho các Microservices:

1. **`UserEvent.java` & `UserEventType.java`:**
   * Các loại sự kiện: `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`, `FRIEND_REQUEST`, `FRIEND_ACCEPTED`.
   * Payload chứa thông tin cốt lõi: `userId`, `username`, `displayName`, `imageId`, `bannerImageId`, `actorId`, `recipientId`.
2. **`ChatEvent.java` & `ChatEventType.java`:**
   * Các loại sự kiện: `MESSAGE_SENT`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`, `CHAT_CREATED`.

---

### B. Luồng Lắng nghe & Phân phối Sự kiện giữa các Microservices

```
                     +-------------------+
                     |   user-service    |
                     +---------+---------+
                               | (Publish: USER_UPDATED)
                               v
                  +-------------------------+
                  |       KAFKA TOPIC       |
                  |     [user-events]       |
                  +----+---------------+----+
                       |               |
       (Consume)       |               |       (Consume)
                       v               v
            +------------------+   +------------------+
            |  search-service  |   | storage-service  |
            +------------------+   +------------------+
            | Cập nhật Index   |   | Xóa ảnh banner/  |
            | Elasticsearch    |   | avatar cũ an toàn|
            +------------------+   +------------------+
```

#### 1. Luồng Người dùng Cập nhật Profile (`USER_UPDATED`):
* **Nhà phát hành (`Producer` - `user-service`):**
  Trong `UserServiceImpl.updateUser()`, sau khi lưu thông tin mới vào PostgreSQL thành công (`userRepository.save(user)`), lập tức gọi:
  ```java
  eventPublisher.publish(UserEvent.builder()
          .type(UserEventType.USER_UPDATED.name())
          .userId(saved.getId())
          .username(saved.getUsername())
          .displayName(saved.getDisplayName())
          .imageId(saved.getImageId())
          .build());
  ```
  API trả về ngay lập tức cho Client với tốc độ cực nhanh ($< 50\text{ms}$).
* **Người tiêu thụ 1 (`Consumer` - `search-service`):**
  `UserEventConsumer` lắng nghe topic `user-events`. Nhận thấy `USER_UPDATED`, gọi `UserIndexService` để cập nhật lập tức tài liệu của User trong **Elasticsearch**, giúp chức năng Tìm kiếm người dùng (`SearchPage`) luôn hiển thị tên và avatar mới nhất.
* **Người tiêu thụ 2 (`Consumer` - `storage-service`):**
  `UserEventConsumer` tại storage lắng nghe `user-events`. Nếu kiểm tra thấy ID ảnh cũ đã bị thay thế hoặc xóa bớt, tiến hành dọn dẹp file rác trên MinIO/Object Storage một cách an toàn mà không làm chậm API cập nhật của người dùng.

#### 2. Luồng Thông báo Đẩy khi Có Tin nhắn mới (`MESSAGE_SENT`):
* **Nhà phát hành (`chat-service`):**
  Trong `MessageServiceImpl.sendMessage()`, sau khi lưu tin nhắn vào DB, kiểm tra nếu người nhận đang **Offline WebSocket**, `chat-service` sẽ bắn sự kiện `MESSAGE_SENT` vào Kafka topic `chat-events`.
* **Người tiêu thụ (`notification-service`):**
  `ChatEventConsumer` tại `notification-service` nhận sự kiện $\rightarrow$ Gọi `UnreadCounterService.increment(userId)` tăng badge đỏ trong Redis $\rightarrow$ Lưu thông báo vào bảng `notifications` $\rightarrow$ Đẩy Push Notification (hoặc SSE) cho người dùng.

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nếu sự kiện bị bắn lỗi hoặc Consumer xử lý thất bại (ví dụ Elasticsearch tại `search-service` bị sập ngang khi đang index), làm sao để không bị mất dữ liệu hay lặp sự kiện mãi mãi?"*
**Cấu trúc trả lời:**
> *"Dạ, để đảm bảo độ tin cậy trong hệ thống Event-Driven với Kafka, em áp dụng các cơ chế chịu lỗi sau:
> 1. **Dead Letter Topic (DLT):** Trong cấu hình `ConcurrentKafkaListenerContainerFactory` của Spring Boot, em thiết lập cơ chế thử lại (`RetryTemplate`). Nếu một Consumer xử lý thất bại 3 lần liên tiếp, sự kiện sẽ được tự động chuyển sang một Topic đặc biệt là `user-events.DLT` (Dead Letter Topic) để quản trị viên kiểm tra và xử lý sau, không làm tắc nghẽn (`Poison Pill`) các sự kiện tiếp theo.
> 2. **Idempotent Consumer (Tính lũy đẳng):** Vì Kafka đảm bảo `At-Least-Once Delivery` (có thể gửi lại 1 sự kiện 2 lần nếu mất mạng lúc commit offset), tại `search-service` hay `storage-service`, logic xử lý luôn được thiết kế **Lũy đẳng (`Idempotent`)**. Cụ thể: Lệnh upsert vào Elasticsearch theo `userId` dù chạy 1 lần hay 10 lần với cùng một payload thì kết quả cuối cùng vẫn giống hệt nhau, không bao giờ bị nhân đôi dữ liệu."*

### ❓ Câu hỏi 2: *"Tại sao em lại tách riêng `user-events` và `chat-events` thành 2 topics khác nhau thay vì bỏ chung vào 1 topic `zchat-all-events`?"*
**Cấu trúc trả lời:**
> *"Dạ, việc tách Topic dựa theo **Lãnh thổ Nghiệp vụ (`Domain Boundaries / Bounded Context`)** trong Microservices mang lại 3 lợi ích vượt trội:
> 1. **Khả năng Mở rộng Độc lập (`Independent Scaling`):** Lưu lượng tin nhắn chat (`chat-events`) cao gấp hàng nghìn lần so với lưu lượng cập nhật thông tin cá nhân (`user-events`). Tách riêng giúp em có thể chia `chat-events` thành **12 Partitions** để chạy 12 Consumer instances song song, trong khi `user-events` chỉ cần **3 Partitions** là đủ.
> 2. **Bảo mật & Phân quyền (`ACLs - Access Control Lists`):** Topic `chat-events` chứa nội dung nhạy cảm của tin nhắn, chỉ cho phép `notification-service` và `analytics-service` đọc. Còn `user-events` chứa thông tin công khai hơn, cho phép `search-service` truy cập tự do.
> 3. **Tránh nhiễu dữ liệu (`Separation of Concerns`):** Consumer không phải mất công deserialization và `if/else` lọc bỏ những sự kiện không liên quan đến nghiệp vụ của mình."*
