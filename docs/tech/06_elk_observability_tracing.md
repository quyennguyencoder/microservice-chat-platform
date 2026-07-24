# Observability Deep-Dive: ELK Stack Logging & Distributed Tracing (`traceId` / `spanId`)

Tài liệu này mô tả chi tiết kiến trúc giám sát và quan sát hệ thống (`Observability`), cách thức thu thập log phân tán qua **ELK Stack (Elasticsearch, Logstash, Kibana)**, và cơ chế truy vết luồng xử lý (`Distributed Tracing`) trong Microservices ZChat.

---

## 1. Tại sao Hệ thống Microservices cần ELK Stack & Distributed Tracing?

### Vấn đề "Mù Log" (`Log Blindness`) trong Microservices:
Trong một ứng dụng Monolithic (đơn khối), tất cả nhật ký (`System Logs`) đều được ghi vào một file `application.log` duy nhất trên một máy chủ. Khi xảy ra lỗi, lập trình viên chỉ cần mở file đó ra và tìm `grep ERROR` là thấy ngay nguyên nhân.

Tuy nhiên, trong kiến trúc ZChat Microservices với **5 microservices độc lập** (`user-service`, `chat-service`, `search-service`, `storage-service`, `notification-service`) cộng thêm `api-gateway`:
* Khi một Request từ người dùng đi qua API Gateway $\rightarrow$ `user-service` $\rightarrow$ bắn Kafka Event sang `search-service` và `storage-service`:
  * Nếu `storage-service` xảy ra lỗi lúc xóa ảnh, làm sao biết Request ban đầu từ người dùng nào gây ra lỗi đó?
  * Nếu phải đăng nhập bằng SSH vào từng container/máy chủ (`service-1`, `service-2`, `service-3`) để đọc từng file log rời rạc thì mất hàng giờ đồng hồ và cực kỳ dễ nhầm lẫn!

### Giải pháp Observability của ZChat:
1. **Thu thập Log Tập trung (`Centralized Logging` - ELK Stack):** Toàn bộ log từ 6 microservices đều được đẩy tự động theo thời gian thực về một kho chứa trung tâm thông qua `LogstashTcpSocketAppender`.
2. **Truy vết Phân tán (`Distributed Tracing` - `traceId` / `spanId`):** Mỗi khi có một Request bước vào `api-gateway`, hệ thống tự động sinh ra một mã định danh duy nhất là **`traceId`**. Mã `traceId` này được truyền đi xuyên suốt qua các lời gọi HTTP chéo và Kafka Events tới từng service con. Nhờ đó, chỉ cần gõ 1 `traceId` vào Kibana, ta thấy ngay toàn bộ hành trình của request đó đi qua những service nào, mất bao nhiêu mili-giây ở đâu!

---

## 2. Chi tiết Triển khai trong Codebase ZChat

### A. Cấu hình đẩy Log tự động sang Logstash (`logback-spring.xml`)
Vị trí file: `common/src/main/resources/logback-spring.xml` & `api-gateway/src/main/resources/logback-spring.xml`

Trong module `common` (được tất cả các microservices kế thừa), file `logback-spring.xml` được cấu hình để gửi trực tiếp log sang **Logstash Server qua cổng TCP (`localhost:5000`)** ở định dạng JSON chuẩn hóa thông qua thư viện `net.logstash.logback.appender.LogstashTcpSocketAppender`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <!-- Appender đẩy log sang Logstash (ELK Stack) qua TCP dưới dạng JSON -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOGSTASH_DESTINATION:-localhost:5000}</destination>
        <ringBufferSize>8192</ringBufferSize>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Tự động đính kèm traceId và spanId từ MDC vào JSON Log -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

#### Ý nghĩa các thông số:
* `<destination>`: Địa chỉ máy chủ Logstash nhận log. Sử dụng biến môi trường `${LOGSTASH_DESTINATION}` để linh hoạt khi triển khai Docker/Kubernetes.
* `<ringBufferSize>8192`: Bộ đệm vòng (`Ring Buffer`) bất đồng bộ trong bộ nhớ RAM, giúp việc ghi log sang Logstash không bao giờ làm chậm (`block`) luồng xử lý chính của ứng dụng Java.
* `<includeMdcKeyName>traceId</includeMdcKeyName>`: Cho phép LogstashEncoder tự động trích xuất các mã `traceId` và `spanId` từ Mapped Diagnostic Context (MDC) của Spring Micrometer Tracing và đóng gói thành thuộc tính JSON gửi sang Kibana.

---

### B. Luồng Truy vết Dữ liệu (`Tracing Flow`) qua Kibana Dashboard

```
[ Frontend Client ] 
        │
        ▼ (HTTP GET /api/v1/users/profile)
+───────────────────────────────────────────────────────────+
│                       API GATEWAY                         │
│ Sinh ra traceId: 8a92c4b1d3ef4012                         │
│ Ghi Log: "Received request..." [traceId=8a92c..., span=01]│
+─────────────────────────────┬─────────────────────────────+
                              │ (Header: X-Trace-Id)
                              ▼
+───────────────────────────────────────────────────────────+
│                      USER-SERVICE                         │
│ Nhận traceId: 8a92c4b1d3ef4012                            │
│ Ghi Log: "Updating profile..." [traceId=8a92c..., span=02]│
+─────────────────────────────┬─────────────────────────────+
                              │ (Kafka Event: USER_UPDATED)
                              ▼
+───────────────────────────────────────────────────────────+
│                     SEARCH-SERVICE                        │
│ Nhận traceId từ Kafka Header: 8a92c4b1d3ef4012           │
│ Ghi Log: "Indexing user ES..." [traceId=8a92c..., span=03]│
+───────────────────────────────────────────────────────────+
```

Khi quản trị viên mở giao diện **Kibana Dashboard** và tìm kiếm query:
`traceId: "8a92c4b1d3ef4012"`

Họ sẽ nhìn thấy danh sách 3 dòng log liên tiếp từ 3 services khác nhau xếp theo đúng thứ tự thời gian:
1. `[api-gateway] [traceId=8a92c..., spanId=01] INFO  - Routing request to user-service`
2. `[user-service] [traceId=8a92c..., spanId=02] INFO  - User profile updated successfully in PostgreSQL`
3. `[search-service] [traceId=8a92c..., spanId=03] INFO  - Successfully indexed user profile into Elasticsearch`

---

## 3. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nếu máy chủ Logstash (ELK Stack) bị sập hoặc quá tải không nhận được log nữa, liệu các Microservices ZChat có bị treo hoặc sập theo không?"*
**Cấu trúc trả lời:**
> *"Dạ, hệ thống ZChat **hoàn toàn không bị ảnh hưởng và không bao giờ bị sập theo**!
> Lý do là trong cấu hình `LogstashTcpSocketAppender` của `logback-spring.xml`, chúng em sử dụng cơ chế đệm bất đồng bộ với `<ringBufferSize>8192</ringBufferSize>`. Khi Logstash bị sập, appender sẽ tạm thời đẩy log vào bộ đệm vòng trong RAM. Nếu bộ đệm đầy, logback sẽ tự động ngắt (`drop`) các dòng log cũ nhất để bảo vệ luồng thực thi chính của ứng dụng Java. Các API nghiệp vụ như gửi tin nhắn hay đăng nhập vẫn tiếp tục chạy với tốc độ tối đa bình thường mà không hề bị nghẽn mạng."*

### ❓ Câu hỏi 2: *"Làm sao mã `traceId` có thể tự động truyền đi xuyên suốt qua cả HTTP API Gateway và các gói tin Kafka Event mà không cần lập trình viên tự cộng thêm vào từng tham số hàm?"*
**Cấu trúc trả lời:**
> *"Dạ, đó là nhờ sức mạnh của hệ sinh thái **Spring Micrometer Tracing (trước đây là Spring Cloud Sleuth)** tích hợp sẵn với Spring Boot 3:
> 1. **Đối với HTTP Requests:** Khi một request vào API Gateway, Micrometer Tracing tự động tạo ra một `traceId` và đặt vào **MDC (Mapped Diagnostic Context - cấu trúc lưu trữ ThreadLocal của Logback)**. Khi dùng `RestTemplate` hay `WebClient` gọi sang service khác, bộ Interceptor tự động bơm header `traceparent` hoặc `X-Trace-Id` vào HTTP Header của request đi. Service đích nhận được tự động trích xuất ra và đưa tiếp vào MDC của nó.
> 2. **Đối với Kafka Events:** Tương tự, `KafkaTemplate` của Spring Cloud Stream/Kafka tự động đóng gói `traceId` từ MDC hiện tại vào trong **Kafka Message Headers**. Khi `KafkaListener` ở `search-service` đọc sự kiện, nó tự động bung header ra và khôi phục lại `traceId` vào thread xử lý $\rightarrow$ Giúp chuỗi truy vết được liền mạch 100% từ đầu đến cuối mà không tốn một dòng code thủ công nào!"*
