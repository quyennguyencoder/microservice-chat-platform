# ZChat Microservices - Bộ Tài liệu Kiến trúc & Chuyên sâu Công nghệ (Technical Masterclass)

Chào mừng bạn đến với bộ tài liệu phân tích chuyên sâu các công nghệ cốt lõi trong hệ sinh thái **ZChat Microservices**. Bộ tài liệu này được thiết kế để giải thích **lý do lựa chọn (`Why`)**, **chi tiết triển khai thực tế trong code (`How & Where`)**, và **phương án bảo vệ kiến trúc khi phỏng vấn (`Interview Defense Q&A`)** cho từng công nghệ.

---

## Danh mục Tài liệu Chuyên sâu theo Công nghệ

| STT | Công nghệ | Tên tài liệu & Liên kết | Nội dung cốt lõi được đề cập |
| :---: | :--- | :--- | :--- |
| **01** | **Redis** | [01_redis_presence_counter.md](./01_redis_presence_counter.md) | Quản lý trạng thái Online/Typing (`Presence`), TTL tự hủy, Bộ đếm thông báo nguyên tử (`Atomic Unread Counter`). |
| **02** | **STOMP WebSocket** | [02_stomp_websocket.md](./02_stomp_websocket.md) | Giao tiếp 2 chiều thời gian thực, JWT Handshake Interceptor (`CONNECT/DISCONNECT`), Throttling & Debouncing gõ phím. |
| **03** | **Apache Kafka** | [03_kafka_event_driven.md](./03_kafka_event_driven.md) | Kiến trúc sự kiện phân tán (`Event-Driven Architecture`), `UserEvent` & `ChatEvent`, Đồng bộ Elasticsearch & MinIO, Dead Letter Topic. |
| **04** | **Spring Boot & Cloud**| [04_spring_boot_microservices.md](./04_spring_boot_microservices.md)| Bản đồ 5 Microservices + Gateway + Registry + Config, Chuẩn hóa xử lý lỗi tập trung (`GlobalExceptionHandler`). |
| **05** | **React & Gestalt UI** | [05_frontend_react_tanstack_gestalt.md](./05_frontend_react_tanstack_gestalt.md) | Pinterest Gestalt UI Design System, Phân chia TanStack Query (Server State) vs Zustand (Client State), Code Splitting (`LazyRoutes`). |
| **06** | **ELK Stack & Sleuth** | [06_elk_observability_tracing.md](./06_elk_observability_tracing.md) | Centralized Logging với Logstash TCP Appender, Truy vết luồng xử lý phân chuyên qua `traceId` / `spanId` trên Kibana. |
| **07** | **Elasticsearch** | [07_elasticsearch_fulltext_search.md](./07_elasticsearch_fulltext_search.md) | Chỉ mục ngược (`Inverted Index`), CQRS Pattern cho tìm kiếm người dùng siêu tốc tại `search-service`. |
| **08** | **Chat Service Core** | [08_chat_service_masterclass.md](./08_chat_service_masterclass.md) | Phân tích 4 tầng kiến trúc (Security, Routing, Core DB, Event-Driven) và luồng tin nhắn thời gian thực của Chat Service. |

---

## Hướng dẫn Ôn tập trước Giờ Phỏng vấn (`Interview Study Guide`)

1. **Hiểu bản chất thay vì học thuộc lòng:** Mỗi file `.md` đều phân tích rất rõ tại sao không dùng giải pháp cũ (như tại sao không dùng `UPDATE` trong PostgreSQL cho Presence, tại sao không dùng Raw WebSocket, tại sao không dùng Redux...). Hãy xoáy sâu vào những lý do này khi trả lời nhà tuyển dụng!
2. **Nắm vững đường đi của dòng dữ liệu (`Data Flow`):**
   * *Luồng nhắn tin:* `React Client` $\rightarrow$ `API Gateway` $\rightarrow$ `chat-service` (lưu PostgreSQL + push STOMP WebSocket) $\rightarrow$ nếu offline: bắn Kafka `MESSAGE_SENT` $\rightarrow$ `notification-service` (tăng Redis Counter).
   * *Luồng cập nhật Profile:* `React Client` $\rightarrow$ `user-service` (lưu PostgreSQL) $\rightarrow$ bắn Kafka `USER_UPDATED` $\rightarrow$ `search-service` (cập nhật Elasticsearch) + `storage-service` (dọn dẹp ảnh cũ).
3. **Mở sẵn các file `Exception Handling` và `WebSocketConfig` trong IDE:** Buổi demo phỏng vấn sẽ cực kỳ thuyết phục nếu bạn có thể mở ngay file `GlobalExceptionHandler.java` (module `common`) và `WebSocketAuthChannelInterceptor.java` (`chat-service`) để chỉ ra cách bạn bắt lỗi và xác thực JWT.

Chúc bạn tự tin tỏa sáng và chinh phục thành công buổi phỏng vấn! 🚀🏆
