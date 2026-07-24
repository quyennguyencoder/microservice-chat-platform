# Spring Boot & Cloud Microservices Architecture Deep-Dive

Tài liệu này mô tả tổng thể kiến trúc, mô hình phân chia dịch vụ (`Service Decomposition`), cấu hình cơ sở hạ tầng Spring Cloud, và cơ chế **Xử lý Ngoại lệ Tập trung (`Centralized Exception Handling`)** trong hệ sinh thái ZChat.

---

## 1. Bản đồ Kiến trúc Tổng thể (Architecture Landscape)

Hệ thống ZChat được xây dựng trên nền tảng **Java 21 (LTS)** và **Spring Boot 3.3.x / Spring Cloud 2023.x**, áp dụng triệt để các mẫu thiết kế Microservices chuẩn mực:

```
                                  +-----------------------+
                                  |     zchat-client      |
                                  |   (React 19 / Vite)   |
                                  +-----------+-----------+
                                              |
                                              | (HTTP / WebSocket)
                                              v
                                  +-----------------------+
                                  |      api-gateway      |
                                  |  (Spring Cloud Gateway|
                                  |   & OpenAPI Aggregator|
                                  +-----------+-----------+
                                              |
       +--------------------+-----------------+--------------------+--------------------+
       |                    |                                      |                    |
       v                    v                                      v                    v
+--------------+     +--------------+                       +--------------+     +--------------+
| user-service |     | chat-service |                       |search-service|     |storage-serv. |
| (PostgreSQL) |     | (PostgreSQL  |                       |(Elasticsearch|     | (MinIO S3    |
+--------------+     |  & Redis)    |                       +--------------+     +--------------+
                     +--------------+
```

### Danh sách Các Module & Nhiệm Vụ:
1. **`config-server` (Cổng 8888):** Quản lý tập trung toàn bộ cấu hình (`application.yml`, `jwt.secret`, `spring.datasource...`) của tất cả các service từ một kho chứa duy nhất.
2. **`discovery-server` (Cổng 8761 - Eureka):** Định tuyến dịch vụ động (`Dynamic Service Discovery`). Các service con khi khởi động tự động đăng ký vào Eureka để nhận diện và gọi chéo nhau không cần hardcode IP/Port.
3. **`api-gateway` (Cổng 8080):** Cổng vào duy nhất cho mọi request từ Frontend. Chịu trách nhiệm định tuyến (`Routing`), kiểm tra Token JWT sơ bộ, CORS, và tổng hợp tài liệu OpenAPI (`/v3/api-docs` Aggregator).
4. **`common` (Shared Library):** Module dùng chung chứa các tiện ích lõi: Cấu hình Security (`JwtDecoder`), Định nghĩa Kafka Events DTO, Tiêu chuẩn JSON Error Response, và **GlobalExceptionHandler**.
5. **`user-service` (Cổng 8081):** Quản lý hồ sơ người dùng (`Profile`), quan hệ bạn bè (`Friendship`), xác thực và định danh (`Authentication & Authorization`). Sử dụng PostgreSQL.
6. **`chat-service` (Cổng 8082):** Quản lý phòng chat, lịch sử tin nhắn, và kết nối STOMP WebSocket. Sử dụng PostgreSQL + Redis (Presence/Typing).
7. **`notification-service` (Cổng 8083):** Quản lý thông báo, bộ đếm chưa đọc (`Unread Counter`). Sử dụng Redis + PostgreSQL.
8. **`search-service` (Cổng 8084):** Bộ máy tìm kiếm tốc độ cao cho người dùng và lịch sử tin nhắn. Sử dụng Elasticsearch.
9. **`storage-service` (Cổng 8085):** Xử lý tải lên/tải xuống hình ảnh, avatar, banner và tạo ảnh thu nhỏ (`Thumbnails`). Sử dụng MinIO (S3-Compatible Object Storage).

---

## 2. Chuẩn hóa Xử lý Ngoại lệ Tập trung (`Centralized Exception Handling`)

### A. Vấn đề của việc Xử lý Ngoại lệ Tự do ở Từng Service:
Trước khi tối ưu, nếu `user-service` ném ra `UserNotFoundException` còn `storage-service` ném ra `ImageStorageException` (kế thừa `RuntimeException`), các ngoại lệ này khi lọt ra ngoài sẽ rơi vào bộ bắt lỗi mặc định của Spring Boot, trả về **HTTP 500 Internal Server Error** kèm chuỗi StackTrace dài dòng, gây mất an toàn bảo mật và làm Frontend không thể hiểu được lỗi gì đang xảy ra.

### B. Giải pháp Tối ưu của ZChat (Module `common`):
Chúng ta đã thiết lập một hệ thống phân cấp lỗi (`Exception Hierarchy`) khắt khe ngay trong module `common`:

```
                           +----------------------+
                           |   RuntimeException   |
                           +----------+-----------+
                                      |
         +----------------------------+----------------------------+
         |                            |                            |
         v                            v                            v
+-----------------------+    +-----------------------+    +-----------------------+
|  BadRequestException  |    |ResourceNotFoundExcept.|    | UnauthorizedException |
|      (HTTP 400)       |    |      (HTTP 404)       |    |      (HTTP 401)       |
+----+-------------+----+    +-----------+-----------+    +-----------------------+
     |             |                     |
     v             v                     v
+----------+ +-----------+     +-------------------+
|SearchEx. | |StorageEx. |     |UserNotFoundExcept.|
|IndexingEx| |           |     |ImageNotFoundExcept|
+----------+ +-----------+     +-------------------+
```

1. **Định nghĩa 3 Base Exceptions chuẩn trong `com.nguyenquyen.common.exception`:**
   * `BadRequestException`: Cho các lỗi nghiệp vụ sai dữ liệu đầu vào.
   * `ResourceNotFoundException`: Cho các lỗi không tìm thấy tài nguyên trong DB/S3/ES.
   * `UnauthorizedException`: Cho các lỗi truy cập trái phép.
2. **Bộ Bắt Lỗi Duy Nhất (`GlobalExceptionHandler.java` trong `common`):**
   Sử dụng annotation `@RestControllerAdvice`. Khi bất kỳ Service nào (`user-service`, `chat-service`...) ném ra một ngoại lệ kế thừa từ 3 Base Exceptions trên, `GlobalExceptionHandler` sẽ tự động bắt lại và chuẩn hóa thành một đối tượng JSON `ErrorResponse` duy nhất:
   ```json
   {
     "status": 404,
     "error": "Not Found",
     "message": "User not found with id: 123e4567-e89b-12d3-a456-426614174000",
     "timestamp": "2026-07-15T19:30:00Z"
   }
   ```
3. **Tuân thủ Trật tự Kế thừa (`Clean Inheritance`):**
   * **`user-service`:** Xóa sạch các class lỗi trùng lặp. Căn chỉnh `UserNotFoundException` kế thừa `ResourceNotFoundException`.
   * **`storage-service`:** Căn chỉnh `ImageNotFoundException extends ResourceNotFoundException` $\rightarrow$ Tự động trả về HTTP 404. Căn chỉnh `ImageStorageException extends BadRequestException` $\rightarrow$ Tự động trả về HTTP 400.
   * **`search-service`:** Căn chỉnh `SearchException` và `IndexingException` kế thừa `BadRequestException` $\rightarrow$ Tự động trả về HTTP 400.

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Tại sao em không viết `GlobalExceptionHandler` riêng cho từng Microservice mà lại đưa vào module `common`?"*
**Cấu trúc trả lời:**
> *"Dạ, việc đưa `GlobalExceptionHandler` vào module `common` và để các Microservices import vào mang lại 3 giá trị kiến trúc vô cùng lớn:
> 1. **Tính Đồng nhất Giao ước (`Contract Consistency`):** Dù Frontend gọi vào `user-service`, `chat-service` hay `storage-service`, cấu trúc JSON khi xảy ra lỗi (`ErrorResponse`) luôn giống hệt nhau 100%. Frontend chỉ cần viết đúng 1 Interceptor trong Axios là có thể xử lý lỗi hiển thị UI cho toàn bộ hệ thống.
> 2. **Dễ bảo trì & Mở rộng (`DRY - Don't Repeat Yourself`):** Khi cần bổ sung thêm field như `traceId` hay `errorCode` vào phản hồi lỗi, em chỉ cần sửa đúng 1 chỗ tại `common`, sau đó chạy build lại là 5 microservices con tự động được nâng cấp.
> 3. **Phân chia Trách nhiệm Rõ ràng (`Separation of Concerns`):** Từng service con chỉ tập trung vào logic nghiệp vụ cốt lõi, khi gặp lỗi chỉ việc `throw new UserNotFoundException(...)` mà không cần quan tâm đến việc đóng gói JSON hay map HTTP status ra sao."*

### ❓ Câu hỏi 2: *"API Gateway của em xử lý CORS và phân phối tài liệu Swagger/OpenAPI như thế nào khi có tới 5 microservices đứng phía sau?"*
**Cấu trúc trả lời:**
> *"Dạ, API Gateway (`api-gateway`) đóng vai trò là một **OpenAPI Aggregator (Trạm Tổng hợp Tài liệu)**:
> - Từng service con (`user-service`, `chat-service`...) đều tự expose OpenAPI docs riêng của mình tại `/v3/api-docs` nhờ thư viện `springdoc-openapi-starter-webmvc-ui`.
> - Tại Gateway, em định nghĩa cấu hình động trong `application.yml` để gom (`aggregate`) toàn bộ các endpoints `/v3/api-docs` của các service phía sau thông qua Eureka Discovery. Khi lập trình viên hoặc Frontend truy cập vào `http://localhost:8080/swagger-ui.html`, họ có thể chọn dropdown list để xem và test trực tiếp API của bất kỳ microservice nào từ một giao diện duy nhất cực kỳ tiện lợi và chuyên nghiệp!"*
