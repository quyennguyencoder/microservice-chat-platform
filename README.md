# ZChat - Microservices Chat Platform

ZChat là một nền tảng nhắn tin thời gian thực được xây dựng theo kiến trúc **Microservices**, sử dụng **Spring Boot 3** ở backend. Hệ thống được thiết kế để đảm bảo tính mở rộng, hiệu năng cao và dễ dàng bảo trì.

## 🌟 Tính năng chính

- **Nhắn tin thời gian thực**: Hỗ trợ chat 1-1 và group chat (sử dụng WebSocket / STOMP).
- **Quản lý người dùng & Xác thực**: Tích hợp SSO với **Keycloak** (OAuth2/OpenID Connect).
- **Tìm kiếm toàn cục**: Tìm kiếm tin nhắn, người dùng nhanh chóng sử dụng **Elasticsearch**.
- **Quản lý tệp tin**: Tải lên và tải xuống hình ảnh, tệp tin sử dụng **MinIO** (Object Storage).
- **Hệ thống thông báo**: Gửi thông báo đẩy (Push notifications) thông qua **Kafka** (Message Broker).
- **Giám sát & Logging**: Tích hợp **Kibana** để phân tích log hệ thống.

---

## 🏗 Kiến trúc hệ thống

Hệ thống bao gồm các thành phần chính sau:

### 1. Backend (Spring Boot & Spring Cloud)
- **`api-gateway`**: Cổng giao tiếp API, định tuyến request và phân giải token.
- **`discovery-server`**: Service Registry sử dụng Netflix Eureka.
- **`config-server`**: Quản lý cấu hình tập trung cho toàn bộ các microservices.
- **`user-service`**: Quản lý thông tin, hồ sơ người dùng và trạng thái (online/offline).
- **`chat-service`**: Xử lý logic tin nhắn, phòng chat và giao tiếp WebSocket.
- **`search-service`**: Đồng bộ dữ liệu vào Elasticsearch và cung cấp API tìm kiếm.
- **`storage-service`**: Quản lý upload/download file qua MinIO.
- **`notification-service`**: Xử lý và gửi thông báo qua email hoặc socket dựa trên event Kafka.
- **`common`**: Thư viện dùng chung (DTOs, Utils, Exceptions) cho các service.

### 2. Cơ sở hạ tầng (Infrastructure)
Các dịch vụ hạ tầng được triển khai qua Docker Compose:
- **PostgreSQL 17**: Cơ sở dữ liệu quan hệ cho User, Chat, Keycloak.
- **Redis 8**: Caching hệ thống.
- **Kafka**: Message Broker (xử lý sự kiện bất đồng bộ).
- **Elasticsearch 8 & Kibana**: Lưu trữ và truy vấn dữ liệu tìm kiếm / Log.
- **MinIO**: Object Storage (S3 compatible) để lưu file đính kèm, avatar.
- **Keycloak 26**: Identity Provider (Xác thực và phân quyền).

---

## 🚀 Hướng dẫn cài đặt và chạy dự án

### Yêu cầu hệ thống (Prerequisites)
- **Java 21**
- **Docker & Docker Compose**
- **Maven**

### 1. Khởi động Infrastructure (Hạ tầng)
Di chuyển vào thư mục gốc của dự án và chạy các container:

```bash
docker-compose up -d
```
*Lưu ý: Quá trình này sẽ khởi chạy PostgreSQL, Redis, Kafka, Elasticsearch, Kibana, MinIO và Keycloak. Script `init-db.sql` và `realm-export.json` sẽ tự động khởi tạo dữ liệu ban đầu cho Keycloak.*

### 2. Khởi động Backend (Microservices)
Theo thứ tự ưu tiên, bạn cần khởi chạy các core services trước, sau đó là các domain services:

1. Chạy **`config-server`** (Đợi cho tới khi báo thành công).
2. Chạy **`discovery-server`** (Eureka).
3. Chạy **`api-gateway`**.
4. Chạy các services còn lại: `user-service`, `chat-service`, `search-service`, `storage-service`, `notification-service`.

Bạn có thể chạy các service này thông qua IDE (IntelliJ IDEA) hoặc dùng Maven:
```bash
mvn spring-boot:run -pl <tên-module>
```

---

## 📚 Thông tin truy cập nội bộ (Local)

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Keycloak Admin Console**: http://localhost:9090 (admin / admin)
- **MinIO Console**: http://localhost:9001 (minioadmin / minioadmin)
- **Kibana**: http://localhost:5601

---

## 🛠 Công nghệ (Tech Stack)

**Backend:**
- Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.x
- Spring Security, OAuth2 Resource Server
- Spring Data JPA, Hibernate, MapStruct, Lombok

**DevOps & Infrastructure:**
- Docker, Docker Compose
- PostgreSQL, Redis, Apache Kafka, Elasticsearch, MinIO, Keycloak

---

## 📄 Giấy phép (License)
Dự án được xây dựng cho mục đích học tập và phát triển cá nhân. (Tùy chỉnh thông tin license của bạn ở đây).
