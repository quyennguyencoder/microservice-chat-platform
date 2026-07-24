# 🧠 Giải phẫu Toàn diện `chat-service`: Từ Code đến Luồng Dữ liệu (Masterclass)

`chat-service` là trái tim của hệ thống ZChat. Nó xử lý hàng nghìn kết nối WebSocket song song, lưu trữ tin nhắn vào cơ sở dữ liệu, và quản lý trạng thái Online/Offline qua Redis. 

Để hiểu tường tận dịch vụ này, chúng ta sẽ bóc tách nó thành **4 Tầng Kiến Trúc (4 Architectural Layers)** và đi dọc theo đường đi của dữ liệu.

---

## 🏗️ TẦNG 1: Khởi tạo và Bảo mật Kết nối (The Setup & Security Layer)

Khi người dùng vừa mở trang web, quá trình kết nối bắt đầu. Tầng này có nhiệm vụ: Mở cổng WebSocket, chặn cửa kiểm tra vé (JWT Token), và ghi chép lại xem ai đang ngồi ở ghế nào (Session).

### 1. `WebSocketConfig.java` (Người mở cổng)
* **`@EnableWebSocketMessageBroker`**: Kích hoạt STOMP over WebSocket.
* **`registerStompEndpoints`**: Mở cổng `/ws/chat`. Đây là URL mà trình duyệt sẽ gọi `new SockJS('/ws/chat')` để nối dây cáp TCP tới Server.
* **`configureMessageBroker`**: Chia luồng (routing). `/topic` (cho Group), `/queue` (cho nhắn riêng 1-1), `/app` (tiền tố khi Client gửi dữ liệu lên).
* **`configureClientInboundChannel`**: Đăng ký `WebSocketAuthChannelInterceptor` làm bảo vệ đứng gác tại cổng.

### 2. `WebSocketAuthChannelInterceptor.java` (Bảo vệ gác cổng)
* Hàm `preSend()`: Lắng nghe mọi gói tin (khung dữ liệu) bay qua dây cáp.
* **Khi nhận lệnh `CONNECT` (Bắt tay lần đầu):** 
  * Nó rút Header `Authorization: Bearer <Token>` ra.
  * Gọi `jwtDecoder.decode(token)` để xác minh vé giả hay thật.
  * Nếu thật: Lấy ra `userId`, gán vào `Principal` (Cấp thẻ căn cước cho Session).
  * Lập tức gọi `sessionManager.registerSession(...)` để ghi danh và `presenceService.updatePresence(...)` để bật đèn xanh Online trên Redis.
* **Khi nhận lệnh `DISCONNECT` (Mất mạng/Tắt web):**
  * Gọi `sessionManager.removeSession(...)` và báo Redis tắt đèn Online.

### 3. `WebSocketSessionManager.java` (Sổ Nam Tào ghi chép Session)
*(File bạn vừa mở xem)*
* Sử dụng bộ nhớ RAM cực nhanh của Server (`ConcurrentHashMap`) để lưu 2 tấm bản đồ:
  * `userSessions`: Ánh xạ `userId` $\rightarrow$ `sessionId` (Ví dụ: Tuấn đang dùng cáp mạng số `session-123`).
  * `sessionUsers`: Ánh xạ ngược `sessionId` $\rightarrow$ `userId`.
* **Tác dụng:** Khi Server muốn gửi tin nhắn cho "Tuấn", nó phải mở sổ này ra xem Tuấn đang nối vào sợi dây cáp (`session`) số mấy để đẩy data vào đúng ống đó.

---

## ⚡ TẦNG 2: Bộ Phân Luồng Trung Tâm (The Dispatcher Layer)

Khi đường ống đã mở, Client bắt đầu gửi tin nhắn lên (bắt đầu bằng tiền tố `/app/chat`).

### 1. `WebSocketController.java` (Tổng đài viên)
File này dùng `@Controller` và `@MessageMapping("/chat")` để tóm gọn mọi gói tin gửi lên.
* Tham số `WsIncomingMessage incoming`: Chứa kiểu tin nhắn (`SEND_MESSAGE`, `TYPING_START`, `MARK_READ`, `HEARTBEAT`), `chatId`, `content`.
* Cấu trúc `switch-case`:
  * Nếu là `HEARTBEAT` $\rightarrow$ Giao cho `presenceService` gia hạn thời gian sống (TTL) trên Redis thêm 60 giây.
  * Nếu là `TYPING_START` $\rightarrow$ Giao cho `presenceService` lưu tên người dùng vào Redis Set của phòng chat đó (TTL 6s). Rồi dùng `redisMessagePublisher` đẩy lệnh `TYPING_START` cho người còn lại để hiện animation gõ phím.
  * Nếu là `SEND_MESSAGE` $\rightarrow$ Gọi Tầng 3 (`MessageService`) để lưu tin nhắn.

---

## 💾 TẦNG 3: Nghiệp vụ và Lưu trữ (The Core Business Layer)

Tầng này tương tác trực tiếp với PostgreSQL Database để lưu trữ vĩnh viễn.

### 1. `MessageServiceImpl.java` (Lưu tin nhắn)
Hàm `sendMessage(chatId, userId, content, type)`:
* Bước 1: Kiểm tra xem User này có nằm trong phòng chat đó không (`chatMemberRepository.existsByChatIdAndUserId`).
* Bước 2: Tạo đối tượng `Message` mới, gọi `messageRepository.save()` để lưu vào bảng `messages`.
* Bước 3: Cập nhật thời gian `updatedAt` và `lastMessageId` của bảng `chats` để phòng chat này tự động nhảy lên đầu danh sách màn hình.
* Bước 4: Trả về `MessageResponse` (chứa `id`, `content`, `createdAt`).

### 2. Quay lại `WebSocketController` (Đẩy tin nhắn đi)
Sau khi `MessageService` lưu DB xong và trả về `MessageResponse`:
* Controller lấy danh sách tất cả các thành viên trong phòng chat (ngoại trừ người vừa gửi).
* **Kiểm tra Sinh-Tử (Online/Offline Check):**
  * Dùng vòng lặp `for` chạy qua các thành viên.
  * Gọi `sessionManager.isUserOnline(recipientId)`.
  * **Trường hợp ONLINE:** Dùng `redisMessagePublisher.sendToUser(recipientId, outgoing)` $\rightarrow$ Tin nhắn được đẩy qua kênh Redis Pub/Sub, server nào đang giữ kết nối của người nhận sẽ tự động bắt lấy và push xuống WebSocket. Màn hình người nhận nảy số tức thì.
  * **Trường hợp OFFLINE:** Chuyển giao nhiệm vụ cho Tầng 4.

---

## 🚀 TẦNG 4: Sự kiện Phân tán (The Event-Driven Layer)

Làm sao người nhận (đang tắt app) biết có người vừa nhắn tin cho mình để hiện thông báo (Push Notification)?

### 1. `ChatEventPublisher.java` (Bưu tá Kafka)
* Nếu người nhận đang Offline, `chat-service` không thể ép WebSocket gửi đi được (do Session ống dẫn không tồn tại). Nó sẽ tạo ra một gói tin sự kiện `MESSAGE_SENT` và bắn vào **Topic Kafka `chat-events`**.
* Bản thân `chat-service` không quan tâm ai sẽ xử lý tiếp theo (Đây gọi là `Decoupling` - Giảm sát ghép).
* Lúc này, `notification-service` đang lắng nghe Topic `chat-events` sẽ ngay lập tức tóm được gói tin này $\rightarrow$ Nó cộng `+1` vào bộ đếm chưa đọc (`Unread Counter`) trên Redis, lưu DB `notifications`, và gọi SSE/Push Notification đẩy xuống điện thoại người nhận.

---

## 🎯 TỔNG KẾT LUỒNG DỮ LIỆU ĐIỂN HÌNH (FLOW SEND_MESSAGE)

Hãy tưởng tượng bạn (An) chat với Bình:
1. **[Tầng 1]** An mở web $\rightarrow$ Interceptor kiểm tra JWT $\rightarrow$ Ghi An vào `WebSocketSessionManager` và bật đèn xanh Online trên Redis.
2. **[Tầng 2]** An gõ "Hello" và bấm Gửi $\rightarrow$ Dữ liệu chạy vào `WebSocketController.handleMessage()` với Type = `SEND_MESSAGE`.
3. **[Tầng 3]** Controller nhờ `MessageService` lưu chữ "Hello" vào PostgreSQL, sau đó lấy danh sách thành viên ra (có Bình).
4. **[Tầng 2]** Controller hỏi `PresenceService` (trên Redis): *"Bình có đang online không?"*
   * Nếu Bình đang nối dây cáp TCP (`isUserOnline = true`): Controller ném gói tin chữ "Hello" vào kênh **Redis Pub/Sub**. Máy chủ nào đang giữ đường ống của Bình sẽ tự động bắt lấy và bơm vào `/queue/chat/Binh` $\rightarrow$ Màn hình Bình nảy số tức thì.
   * Nếu Bình đang tắt web (`isUserOnline = false`): Controller chuyển sang Tầng 4.
5. **[Tầng 4]** Controller gọi `KafkaTemplate` ném sự kiện "An vừa gửi tin nhắn cho Bình" vào Topic `chat-events`. Phía bên kia `notification-service` sẽ bắt lấy và hiển thị thông báo đỏ lên điện thoại của Bình.

*(Toàn bộ quá trình từ lúc An bấm gửi đến lúc Bình nhận được chỉ tốn khoảng `50ms` - `100ms`!)*
