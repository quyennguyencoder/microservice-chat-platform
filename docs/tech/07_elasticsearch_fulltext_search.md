# Elasticsearch Deep-Dive: Bộ máy Tìm kiếm Toàn văn (Full-Text Search Engine)

Tài liệu này mô tả chi tiết kiến trúc, lý do lựa chọn, và cách thức vận hành bộ máy tìm kiếm tốc độ cao **Elasticsearch** tại `search-service` trong hệ thống ZChat.

---

## 1. Tại sao ZChat sử dụng Elasticsearch thay vì `LIKE '%query%'` trong PostgreSQL?

### Vấn đề của câu truy vấn SQL `LIKE '%keyword%'`:
Giả sử ZChat có 1 triệu người dùng và 50 triệu tin nhắn. Khi người dùng gõ từ khóa `"Nguyễn"` vào thanh tìm kiếm (`SearchPage`):
* **Nếu dùng PostgreSQL:** Lệnh `SELECT * FROM users WHERE display_name LIKE '%Nguyễn%'` buộc cơ sở dữ liệu phải thực hiện **Quét toàn bộ bảng (`Full Table Scan`)**, vì chỉ mục (`B-Tree Index`) bình thường không thể hoạt động với tiền tố `%` ở đầu từ khóa.
* **Hậu quả:** Thời gian trả kết quả mất tới vài giây, làm nghẽn CPU của Database chính và gây chậm trễ cho cả các API nghiệp vụ quan trọng khác như gửi tin nhắn hay đăng nhập!

### Giải pháp với Elasticsearch (`Inverted Index`):
* Elasticsearch sử dụng cấu trúc **Chỉ mục Ngược (`Inverted Index`)**: Thay vì lưu `Tài liệu -> Nội dung`, nó tách từ (`tokenization`) và lưu trữ dưới dạng `Từ khóa -> Danh sách ID tài liệu chứa từ khóa đó`.
* **Hiệu năng vượt trội:** Khi tìm từ `"Nguyễn"`, Elasticsearch lập tức tra cứu trong bảng từ điển (`Terms Dictionary`) và trả về danh sách ID chỉ trong **dưới 10 mili-giây ($< 10\text{ms}$)**.
* **Tính năng Tìm kiếm Nâng cao:** Hỗ trợ tìm kiếm mờ (`Fuzzy Search` - gõ sai chính tả "Nguyen" vẫn ra "Nguyễn"), tìm kiếm theo cụm từ (`Phrase Match`), và tự động chấm điểm liên quan (`Relevance Scoring - TF/IDF`).

---

## 2. Chi tiết Triển khai trong Codebase ZChat (`search-service`)

### A. Đồng bộ Dữ liệu Thời gian thực qua Kafka (`UserEventConsumer`)
Elasticsearch ở `search-service` đóng vai trò là một **Cơ sở dữ liệu Đọc tối ưu (`Read-Optimized Store - CQRS Pattern`)**. Dữ liệu gốc (`Single Source of Truth`) vẫn nằm trong PostgreSQL tại `user-service`.

#### Luồng đồng bộ:
1. Khi có người dùng mới tạo tài khoản hoặc cập nhật profile, `user-service` lưu vào PostgreSQL thành công và bắn sự kiện `USER_CREATED` / `USER_UPDATED` vào Kafka Topic `user-events`.
2. Tại `search-service`, `UserEventConsumer` nhận sự kiện $\rightarrow$ Gọi `UserIndexRepository.save()` để tự động tạo/cập nhật tài liệu (`Document`) vào chỉ mục (`Index`) `users` trong Elasticsearch.
3. Khi người dùng tìm kiếm trên giao diện Frontend (`SearchPage`), request được gửi thẳng tới API của `search-service` (`GET /api/v1/search/users?q=...`) mà không cần đụng chạm gì đến `user-service` hay PostgreSQL!

---

### B. Chuẩn hóa Ngoại lệ và Xử lý Lỗi Nghiệp vụ
Vị trí folder: `search-service/src/main/java/com/nguyenquyen/searchservice/exception/`

Sau đợt tối ưu kiến trúc vừa rồi, các ngoại lệ đặc thù của bộ máy tìm kiếm đều được kế thừa từ chuẩn `common`:
* **`SearchException extends BadRequestException`**: Được throw ra khi người dùng gửi chuỗi truy vấn sai cú pháp, chứa ký tự đặc biệt gây lỗi Elasticsearch Query Parser. `GlobalExceptionHandler` tự động chuyển thành **HTTP 400 Bad Request**.
* **`IndexingException extends BadRequestException`**: Được throw ra khi quá trình ánh xạ (`mapping`) hoặc ghi dữ liệu tài liệu vào Elasticsearch gặp sự cố trường dữ liệu không tương thích.

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nếu `search-service` (hoặc Elasticsearch) bị sập hoàn toàn, liệu người dùng ZChat có bị gián đoạn chức năng gửi/nhận tin nhắn hay không?"*
**Cấu trúc trả lời:**
> *"Dạ, kiến trúc ZChat áp dụng mô hình **Phân tách Trách nhiệm Lệnh và Truy vấn (`CQRS - Command Query Responsibility Segregation`)** kết hợp **Độ nhạy cảm Lỏng (`Loose Coupling`)** qua Kafka:
> - Khi `search-service` bị sập, toàn bộ các chức năng cốt lõi (`Core Business`) của ZChat như Đăng nhập, Gửi/Nhận tin nhắn thời gian thực qua WebSocket, Upload ảnh... tại `chat-service` và `user-service` **vẫn hoạt động bình thường 100%**!
> - Người dùng chỉ tạm thời không thể tìm kiếm bạn bè mới trên thanh Search. Các sự kiện `USER_UPDATED` vẫn được giữ an toàn trong Kafka Topic. Khi `search-service` được khởi động lại, nó sẽ tự động đọc tiếp (`Consume`) từ offset cuối cùng để đồng bộ đầy đủ dữ liệu mới nhất vào Elasticsearch."*

### ❓ Câu hỏi 2: *"Khi một người dùng đổi tên từ 'Tuấn' sang 'An', làm sao em đảm bảo Elasticsearch không bị lưu trùng lặp thành 2 tài khoản?"*
**Cấu trúc trả lời:**
> *"Dạ, để đảm bảo tính duy nhất, em thiết lập **Document ID của Elasticsearch trùng khớp chính xác 100% với Primary Key (`UUID`) của User trong PostgreSQL**.
> Cụ thể, khi `search-service` lắng nghe sự kiện `USER_UPDATED` từ Kafka, lệnh ghi vào Elasticsearch được thực hiện dưới dạng **Upsert (`Update or Insert` theo ID)**:
> `userIndexRepository.save(new UserDocument(event.getUserId(), ...))`
> Vì Document ID (`event.getUserId()`) đã tồn tại, Elasticsearch sẽ tự động ghi đè (`Overwrite`) tài liệu cũ của 'Tuấn' thành 'An' chứ không bao giờ sinh ra bản ghi mới, đảm bảo tính nhất quán cuối cùng (`Eventual Consistency`) tuyệt đối cho hệ thống."*
