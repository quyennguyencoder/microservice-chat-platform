# STOMP over WebSocket Deep-Dive: Nhắn tin Thời gian thực & Xác thực JWT Handshake

Tài liệu này mô tả chi tiết kiến trúc, cơ chế bảo mật, và luồng giao tiếp hai chiều thời gian thực (**Real-time Bi-directional Communication**) thông qua giao thức **STOMP over WebSocket** trong `chat-service` và `zchat-client`.

---

## 1. Tại sao sử dụng STOMP over WebSocket thay vì Raw WebSocket hay HTTP Long-Polling?

### So sánh các công nghệ Real-time:
1. **HTTP Long-Polling / Short-Polling:**
   * Client liên tục gửi HTTP request (`GET /messages`) mỗi vài giây để hỏi server có tin nhắn mới không.
   * **Nhược điểm:** Tốn băng thông khổng lồ cho HTTP Header, lãng phí tài nguyên CPU/DB khi 90% request trả về rỗng, độ trễ cao.
2. **Raw WebSocket (WebSocket Thuần):**
   * Mở một đường ống TCP kết nối hai chiều liên tục giữa Client và Server.
   * **Nhược điểm:** WebSocket thuần chỉ truyền các gói tin dạng Chuỗi thô (Text/Binary payload). Lập trình viên phải tự chế ra quy ước định dạng JSON, tự viết logic định tuyến (gửi cho ai, vào phòng nào, cơ chế pub/sub), rất dễ sinh lỗi và khó mở rộng.
3. **STOMP over WebSocket (`Simple Text Oriented Messaging Protocol`):**
   * STOMP hoạt động như một lớp giao thức cấp cao (Application-layer Protocol) chạy trên nền đường ống WebSocket.
   * Biến WebSocket thành một hệ thống **Message Broker (Pub/Sub)** thực thụ với các định dạng chuẩn:
     * `DESTINATION`: Định tuyến kênh tin nhắn (Ví dụ: `/topic/chat/123` cho Group, `/queue/chat/{userId}` cho cá nhân).
     * `HEADERS`: Cho phép gửi kèm Metadata và Token JWT xác thực trong từng gói tin STOMP.
     * `BODY`: Nội dung JSON payload.

---

## 2. Chi tiết Triển khai trong Codebase ZChat

### A. Cấu hình & Bộ định tuyến WebSocket (`WebSocketConfig.java` - `chat-service`)
Vị trí file: `chat-service/src/main/java/com/nguyenquyen/chatservice/config/WebSocketConfig.java`

* **Endpoint kết nối (`registerStompEndpoints`):** `/ws/chat` (Hỗ trợ `setAllowedOriginPatterns("*")` để cho phép Client từ các tên miền khác kết nối tới).
* **Quy ước Message Broker (`configureMessageBroker`):**
  * `enableSimpleBroker("/topic", "/queue", "/user")`: Các tiền tố kênh ra (Outgoing Channel) để Server đẩy tin nhắn về cho Client.
  * `setApplicationDestinationPrefixes("/app")`: Tiền tố kênh vào (Incoming Channel) khi Client gửi tin nhắn lên Server (Ví dụ Client gửi vào `/app/chat`).
  * `setUserDestinationPrefix("/user")`: Tiền tố định tuyến tin nhắn riêng cho từng cá nhân (`/user/{userId}/queue/...`).

---

### B. Bảo mật Kết nối với JWT Handshake Interceptor (`WebSocketAuthChannelInterceptor.java`)
Vị trí file: `chat-service/src/main/java/com/nguyenquyen/chatservice/websocket/WebSocketAuthChannelInterceptor.java`

Vì WebSocket là giao thức kết nối kéo dài (`Persistent Connection`), việc xác thực không thể chỉ làm ở tầng HTTP Filter thông thường. ZChat sử dụng **`ChannelInterceptor`** để kiểm soát từng khung tin STOMP ngay tại cổng vào của `clientInboundChannel`.

#### Cơ chế xử lý (`preSend`):
1. **Lắng nghe lệnh `STOMP CONNECT`:** Khi Client vừa mở đường ống WebSocket và gửi gói tin STOMP đầu tiên (`CONNECT`), Interceptor lấy ra Header `Authorization: Bearer <token>`.
2. **Giải mã & Xác thực (`jwtDecoder.decode`):**
   * Nếu Token hợp lệ: Lấy ra `userId (Subject)`, gán đối tượng `Principal` vào `StompHeaderAccessor.setUser()`. Đồng thời ghi nhận Session vào `sessionManager.registerSession(sessionId, userId)` và lập tức đánh dấu User Online qua `presenceService.updatePresence(userId)`.
   * Nếu Token sai/hết hạn: Ném ra ngoại lệ, Spring lập tức ngắt đường ống WebSocket $\rightarrow$ Bảo vệ hệ thống tuyệt đối khỏi các kết nối trái phép.
3. **Lắng nghe lệnh `STOMP DISCONNECT`:** Khi Client tắt tab hoặc mất mạng, `handleDisconnect` được gọi $\rightarrow$ Hủy Session trong `sessionManager` và gọi `presenceService.removePresence(userId)` để chuyển trạng thái sang Offline.

---

### C. Bộ Điều hướng & Xử lý Giao tiếp (`WebSocketController.java`)
Vị trí file: `chat-service/src/main/java/com/nguyenquyen/chatservice/websocket/WebSocketController.java`

Lắng nghe tất cả các sự kiện gửi lên kênh `/app/chat` thông qua `@MessageMapping("/chat")` với payload là `WsIncomingMessage`. Phân loại theo `incoming.getType()`:

| Loại Sự kiện (`MessageType`) | Logic Xử lý tại Backend | Phản hồi ra Kênh WebSocket (`WsOutgoingMessage`) |
| :--- | :--- | :--- |
| `SEND_MESSAGE` | Gọi `messageService.sendMessage(...)` để lưu tin nhắn vào PostgreSQL. Kiểm tra danh sách thành viên trong phòng: Nếu thành viên đang **Online (`sessionManager.isUserOnline`)**, push tin nhắn ngay lập tức qua `/queue/chat/{recipientId}`. Nếu **Offline**, chuyển nhiệm vụ cho Kafka/MessageService đẩy thông báo. | Push `WsOutgoingMessage.newMessage(message)` cho cả người gửi và người nhận Online. |
| `TYPING_START` | Gọi `presenceService.startTyping(userId, chatId)` lưu vào Redis Set với TTL 6 giây. | Push `WsOutgoingMessage.typing(chatId, userId)` tới các thành viên khác đang Online trong phòng. |
| `TYPING_STOP` | Xóa `userId` khỏi Redis Set của phòng chat. | Push `WsOutgoingMessage.stoppedTyping(chatId, userId)` cho các thành viên khác tắt animation gõ phím. |
| `MARK_READ` | Cập nhật trạng thái `is_read = true` cho các tin nhắn trong DB thông qua `messageService.markAsRead()`. | Push `WsOutgoingMessage.messagesRead(chatId, userId)` báo cho người gửi biết tin nhắn đã chuyển sang **"Đã xem"**. |
| `HEARTBEAT` | Gọi `presenceService.updatePresence(userId)` gia hạn Redis TTL của người dùng thêm 60 giây. | Không push (chỉ xử lý gia hạn ngầm). |

---

### D. Tối ưu Hiệu năng ở Client (`MessageInput.tsx` & `useChatWebSocket.ts`)

#### 1. Kỹ thuật Throttling & Debouncing cho Typing Indicator (`MessageInput.tsx`):
* **Vấn đề:** Nếu người dùng gõ 10 phím/giây và mỗi phím đều bắn sự kiện `TYPING_START` qua WebSocket, mạng sẽ bị bão hòa (Network Flooding).
* **Giải pháp ZChat:**
  * **Throttling (`lastSentRef` - 2.5 giây):** Dù gõ nhanh đến đâu, Client chỉ được phép gửi `TYPING_START` tối đa **1 lần mỗi 2.5 giây**.
  * **Debouncing (`timeoutRef` - 2 giây):** Nếu sau 2 giây kể từ phím gõ cuối cùng không có thêm thao tác nào, Client mới tự động bắn event `TYPING_STOP`.

#### 2. Kỹ thuật Tự động Dọn dẹp Trạng thái ảo (`useChatWebSocket.ts`):
* Khi Client nhận được sự kiện `USER_TYPING` từ người khác, nó hiển thị animation gõ phím và đồng thời cài một bộ đếm hẹn giờ `setTimeout(..., 5000)` (5 giây).
* Nếu vì lý do mất mạng mà sự kiện `USER_STOPPED_TYPING` không đến được, sau đúng 5 giây, giao diện tự động dọn dẹp và ẩn animation gõ phím đi.

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nếu Token JWT của người dùng hết hạn ngay trong lúc đường ống WebSocket đang mở (ví dụ sau 30 phút nối dây), hệ thống xử lý thế nào?"*
**Cấu trúc trả lời:**
> *"Dạ, WebSocket là kết nối lâu dài (`Persistent Connection`), việc kiểm tra Token lúc `STOMP CONNECT` ban đầu chỉ bảo vệ cổng vào. Trong ZChat, chúng ta xử lý bảo mật toàn diện bằng 2 lớp:
> 1. Khi JWT hết hạn, bất kỳ HTTP API nào gọi song song (như lấy lịch sử chat, upload ảnh) đều trả về `401 Unauthorized` từ API Gateway $\rightarrow$ Axios Interceptor của Frontend (`authService`) sẽ tự động gọi API Refresh Token hoặc đăng xuất người dùng.
> 2. Nếu đăng xuất hoặc Refresh Token thất bại, Frontend sẽ lập tức gọi hàm `websocketService.disconnect()` để ngắt toàn bộ đường ống STOMP WebSocket hiện tại."*

### ❓ Câu hỏi 2: *"Khi hệ thống mở rộng (`Scale Out`) chạy 3 Instances của `chat-service` sau Load Balancer, nếu User A nối WebSocket vào Instance 1, User B nối vào Instance 2, thì User A gửi tin nhắn qua WebSocket làm sao User B nhận được?"*
**Cấu trúc trả lời:**
> *"Dạ, đây chính là bài toán phân tán WebSocket Session. Nếu dùng `SimpMessagingTemplate` thông thường, nó chỉ lưu session trong bộ nhớ cục bộ của từng instance nên Instance 1 sẽ không thể gửi tin cho User B. Để giải quyết triệt để, kiến trúc ZChat đã được thiết kế sử dụng cơ chế **Redis Pub/Sub**:
> Khi User A gửi tin vào Instance 1, thay vì gửi trực tiếp, Instance 1 sử dụng `RedisMessagePublisher` để đẩy gói tin STOMP đó vào một kênh Redis (`ws-chat-channel`). Tất cả các Instances (`chat-service 1, 2, 3`) đều có một `RedisMessageSubscriber` lắng nghe kênh này. Instance 2 khi nhận được tin nhắn từ Redis sẽ tra cứu trong RAM nội bộ (`WebSocketSessionManager`), phát hiện User B đang giữ kết nối WebSocket tại máy mình và lập tức push tin nhắn xuống cho User B."*
