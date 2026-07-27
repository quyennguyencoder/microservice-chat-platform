# Data Flows & API Interactions (Luồng tương tác Frontend - Backend)

Tài liệu này mô tả chi tiết các luồng nghiệp vụ (Data Flows) quan trọng nhất trong ZChat. Nó diễn giải quá trình từ lúc người dùng thao tác trên giao diện (React/Frontend), các API nào được gọi xuống API Gateway, và cách các Microservices ở Backend phối hợp xử lý.

---

## 1. Luồng Xác thực và Khởi tạo Ứng dụng (Authentication & App Initialization)

Đây là luồng xảy ra ngay khi người dùng mở trang web ZChat.

### Sơ đồ Luồng (Sequence)
1. **Frontend (React)**: Kiểm tra JWT Token trong LocalStorage/Zustand.
2. Nếu chưa có / Hết hạn: Frontend chuyển hướng sang trang đăng nhập của **Keycloak** (OIDC Flow).
3. Sau khi đăng nhập Keycloak thành công, Frontend nhận được `AccessToken` (JWT).
4. **Frontend $\rightarrow$ API Gateway**: Gọi API `GET /api/v1/users/me` kèm Header `Authorization: Bearer <Token>`.
5. **API Gateway**:
   - Xác thực chữ ký JWT hợp lệ.
   - Định tuyến request xuống `user-service`.
6. **user-service**:
   - Nếu user này chưa có trong DB (Lần đầu đăng nhập bằng Keycloak): Lấy email/name từ JWT claims và lưu mới vào PostgreSQL $\rightarrow$ Trả về `201 Created`.
   - Nếu đã có: Trả về thông tin Profile $\rightarrow$ Trả về `200 OK`.
7. **Frontend**:
   - Lưu thông tin Profile vào Zustand (Global State).
   - Khởi tạo kết nối STOMP WebSocket: Gọi `new SockJS('http://localhost:8080/ws/chat')`.
8. **chat-service (WebSocket Interceptor)**:
   - Bắt gói tin `CONNECT`.
   - Giải mã JWT, lấy `userId`.
   - Lưu Session vào RAM cục bộ (`WebSocketSessionManager`).
   - Cập nhật trạng thái lên **Redis** (Tự động Online trong 60s).
   - Phát sóng (Broadcast) trạng thái Online qua **Redis Pub/Sub** để thông báo cho bạn bè.

---

## 2. Luồng Nhắn tin Cá nhân / Nhóm (Real-time Messaging)

Luồng phức tạp nhất hệ thống, kết hợp giữa Database, WebSocket, Redis và Kafka.

### Sơ đồ Luồng (Sequence)
1. **Frontend (Người gửi)**: Nhập nội dung chat và bấm Gửi.
2. **Frontend $\rightarrow$ WebSocket (chat-service)**: Bắn gói tin STOMP `SEND_MESSAGE` vào kênh `/app/chat` chứa `chatId`, `content`, `type`.
3. **chat-service**:
   - Lưu tin nhắn vào **PostgreSQL** (`messages` table).
   - Cập nhật thời gian `updatedAt` của phòng chat.
   - Trích xuất danh sách tất cả thành viên trong phòng (trừ người gửi).
4. **Xử lý Thời gian thực (Redis Pub/Sub)**:
   - `chat-service` ném gói tin `NEW_MESSAGE` lên kênh Redis `ws-chat-channel`.
   - TẤT CẢ các instances của `chat-service` bắt được gói tin này.
   - Instance nào đang "ôm" kết nối mạng của người nhận (Bình) sẽ lập tức bơm tin nhắn xuống kênh `/queue/chat/Binh`.
   - Trình duyệt của Bình nhận tin nhắn và hiển thị lên màn hình (`<100ms`).
5. **Xử lý Bất đồng bộ (Kafka)**:
   - Cùng lúc đó, `chat-service` ném một sự kiện `NEW_MESSAGE` vào **Kafka Topic `chat-events`**.
6. **notification-service & search-service (Kafka Consumers)**:
   - **`notification-service`** bắt sự kiện: Tăng bộ đếm chưa đọc (`Unread Counter`) trên **Redis** của Bình lên `+1`. Nếu Bình đang offline, gọi dịch vụ Push Notification (FCM/Email) gửi thông báo về điện thoại.
   - **`search-service`** bắt sự kiện: Bóc tách nội dung tin nhắn và đẩy vào **Elasticsearch** để đánh chỉ mục, phục vụ tính năng tìm kiếm lịch sử chat sau này.

---

## 3. Luồng Lấy Lịch sử Tin nhắn (Paging Messages)

Khi người dùng mở một phòng chat hoặc cuộn màn hình lên trên cùng.

### Sơ đồ Luồng (Sequence)
1. **Frontend**: React Component mount hoặc thanh cuộn (Scroll) chạm nóc. `TanStack Query` kích hoạt hook `useInfiniteQuery`.
2. **Frontend $\rightarrow$ API Gateway**: Gọi API `GET /api/v1/chats/{chatId}/messages?page=0&size=50` (REST HTTP, không qua WebSocket).
3. **API Gateway**: Route request sang `chat-service`.
4. **chat-service**:
   - Kiểm tra quyền: JWT `userId` có nằm trong danh sách thành viên của `chatId` không?
   - Truy vấn **PostgreSQL** lấy 50 tin nhắn gần nhất, sắp xếp giảm dần theo thời gian.
5. **chat-service $\rightarrow$ Frontend**: Trả về cục JSON.
6. **Frontend**:
   - Gọi hàm đánh dấu đã đọc (bắn WebSocket `MARK_READ` ngầm).
   - `TanStack Query` lưu cache danh sách tin nhắn.
   - Giao diện render 50 tin nhắn ra màn hình.

---

## 4. Luồng Tương tác "Đang gõ phím..." (Typing Indicator)

Luồng tối ưu hoá hiệu năng cực cao.

### Sơ đồ Luồng (Sequence)
1. **Frontend (Người gõ)**: Bắt đầu gõ phím. Cứ mỗi lần gõ, hàm `onKeyDown` kích hoạt. Kỹ thuật **Throttling** chặn lại, chỉ cho phép bắn sự kiện 2.5 giây/lần.
2. **Frontend $\rightarrow$ WebSocket (chat-service)**: Bắn STOMP `TYPING_START` vào `/app/chat`.
3. **chat-service**:
   - Nhận tín hiệu, đẩy tên User vào một Set trên **Redis** với Key là `typing:chat:{chatId}` và gán **TTL 6 giây**.
   - Bắn tín hiệu `USER_TYPING` lên kênh **Redis Pub/Sub**.
4. **chat-service (của người nhận)**: Bắt được tín hiệu từ Redis Pub/Sub, push về màn hình Frontend người nhận.
5. **Frontend (Người nhận)**:
   - Hiển thị UI: *"An đang gõ..."*
   - Kỹ thuật **Tự hủy ảo**: Cài đặt một `setTimeout` 5 giây. Nếu sau 5 giây không nhận được thêm tín hiệu gõ nào từ An, tự động ẩn UI đi (đề phòng An bị đứt cáp mất mạng).
6. **Frontend (Người gõ)**: Khi ngừng gõ 2 giây (Kỹ thuật **Debouncing**), Frontend bắn STOMP `TYPING_STOP`. `chat-service` nhận được sẽ xóa ID khỏi Redis và báo cho người nhận tắt UI.

---

## 6. Luồng Upload File / Gửi tin nhắn Hình ảnh (MinIO S3)

Luồng xử lý file tĩnh tách biệt với WebSocket để tối ưu băng thông.

### Sơ đồ Luồng (Sequence)
1. **Frontend**: Người dùng chọn một bức ảnh và bấm Gửi.
2. **Frontend $\rightarrow$ API Gateway**: Gọi API `POST /api/v1/storage/images` (Multipart/form-data) gửi trực tiếp file ảnh.
3. **storage-service**:
   - Nhận file, kiểm tra định dạng và kích thước hợp lệ.
   - Upload file lên **MinIO** (S3-compatible Object Storage).
   - Lưu metadata (ID ảnh, URL, kích thước) vào PostgreSQL của `storage-service`.
   - Trả về `imageId` (UUID) cho Frontend.
4. **Frontend $\rightarrow$ WebSocket (chat-service)**: 
   - Sau khi có `imageId`, Frontend bắn gói tin `SEND_MESSAGE` qua WebSocket, nhưng lần này thuộc tính `type: IMAGE` và đính kèm `imageId` thay vì text content dài.
5. **chat-service & Frontend (Người nhận)**:
   - Xử lý gửi tin nhắn y hệt Luồng 2. 
   - Khi Frontend người nhận nhận được tin nhắn báo có ảnh (`type: IMAGE`), nó lấy `imageId` và gọi lại URL tĩnh của MinIO (hoặc qua `storage-service`) để render bức ảnh ra UI.

---

## 7. Luồng Quản lý Nhóm Chat (Group Management)

Chứng minh khả năng xử lý quyền hạn và thông báo Broadcast cấp độ nhóm.

### Sơ đồ Luồng (Sequence)
1. **Frontend (Admin nhóm)**: Chọn một User khác và bấm "Thêm vào nhóm".
2. **Frontend $\rightarrow$ API Gateway**: Gọi API `POST /api/v1/chats/{chatId}/members` kèm ID của người được thêm.
3. **API Gateway**: Route xuống `chat-service`.
4. **chat-service**:
   - **Bảo mật:** Kiểm tra User hiện tại có phải là Admin/Owner của nhóm `chatId` hay không (`chatMemberRepository`).
   - Nếu hợp lệ: Lưu người mới vào PostgreSQL.
   - **Xử lý Thời gian thực (Redis Pub/Sub):** Bắn một tín hiệu System Message dạng `MEMBER_ADDED` lên kênh Redis. Tất cả những người đang mở nhóm chat này sẽ thấy một dòng chữ nhỏ hiện lên *"An vừa thêm Bình vào nhóm"*.
   - **Xử lý Bất đồng bộ (Kafka):** Ném sự kiện `GROUP_UPDATED` vào Kafka.
5. **search-service (Kafka Consumer)**:
   - Bắt sự kiện, cập nhật lại dữ liệu quyền truy cập của nhóm chat trong **Elasticsearch** (Để Bình bắt đầu có thể tìm kiếm được nội dung của nhóm này).

---

## 8. Luồng Tìm kiếm Siêu Tốc (Elasticsearch CQRS)

Áp dụng mô hình CQRS (Command Query Responsibility Segregation) chia tách DB Ghi (PostgreSQL) và DB Đọc (Elasticsearch).

### Sơ đồ Luồng (Sequence)
1. **Frontend**: Người dùng nhập chữ "Họp dự án" vào thanh tìm kiếm. Kỹ thuật Debouncing đợi 300ms sau khi ngừng gõ mới gọi API để tránh spam.
2. **Frontend $\rightarrow$ API Gateway**: Gọi API `GET /api/v1/search/messages?query=Họp+dự+án`.
3. **API Gateway**: Route xuống `search-service`.
4. **search-service**:
   - Trích xuất `userId` từ JWT.
   - Xây dựng câu truy vấn (Elasticsearch Query DSL): Tìm kiếm chuỗi "Họp dự án" (áp dụng bộ phân tích ngôn ngữ - Analyzer/Tokenizer) CHỈ TRONG các phòng chat mà `userId` này đang là thành viên (Lọc quyền truy cập).
   - **Elasticsearch**: Phản hồi kết quả cực nhanh (chỉ vài chục mili-giây) nhờ cấu trúc **Inverted Index** (Chỉ mục ngược).
5. **search-service $\rightarrow$ Frontend**: Trả về danh sách kết quả chứa nội dung đã được Highlight (đánh dấu đậm từ khóa) để Frontend render lên màn hình ngay lập tức.