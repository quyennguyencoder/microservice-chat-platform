# 📖 Sổ Tay Hướng Dẫn Sử Dụng & Vận Hành Hệ Thống Quan Trắc Enterprise (Observability Stack)

Tài liệu này cung cấp hướng dẫn thực chiến từng bước để lập trình viên và quản trị viên hệ thống (DevOps) khai thác tối đa sức mạnh của bộ tứ quan trắc **Zipkin - Grafana - Prometheus - Kibana** trong dự án **ZChat**.

---

## 1. 🚀 Khởi Động & Kiểm Tra Sức Khỏe Hạ Tầng

### Khởi chạy Stack bằng Docker Compose
Mở Terminal tại thư mục gốc dự án và chạy lệnh:
```bash
docker-compose up --build -d
```

### Bảng Kiểm Tra Trạng Thái Cổng Vận Hành (Health Check Table)
Hãy đảm bảo các dịch vụ dưới đây đều ở trạng thái `Running` (Up):

| Dịch vụ | Cổng (Port) | URL Truy cập | Tài khoản mặc định | Ý nghĩa chức năng |
| :--- | :---: | :--- | :---: | :--- |
| **OTel Collector** | `4318` / `4317` | `http://localhost:4318` | *Không có* | Trạm bưu điện trung tâm nhận OTLP Traces/Metrics/Logs |
| **Zipkin UI** | `9411` | `http://localhost:9411` | *Không có* | Giao diện theo dõi thời gian phản hồi API (Tracing Waterfall) |
| **Grafana UI** | `3001` | `http://localhost:3001` | `admin` / `admin` | Táp-lô biểu đồ sinh tồn Server & API (Metrics Visualization) |
| **Prometheus** | `9091` | `http://localhost:9091` | *Không có* | Kho lưu trữ time-series nhận dữ liệu Remote Write |
| **Kibana UI** | `5601` | `http://localhost:5601` | *Không có* | Cổng tra cứu tìm kiếm log tập trung (Log Analytics) |
| **Elasticsearch**| `9200` | `http://localhost:9200` | *Không có* | Kho lưu trữ log full-text (Đã bật Healthcheck) |

---

## 2. 🕵️ Hướng Dẫn Phân Tích Dấu Vết với Zipkin (`:9411`)

Khi một người dùng bấm gửi tin nhắn hoặc cập nhật ảnh đại diện, request đó có thể đi qua `api-gateway` ➔ `user-service` ➔ `storage-service`. Zipkin giúp bạn nhìn thấy toàn bộ hành trình này.

### Cách tra cứu một API chậm / lỗi:
1. Mở trình duyệt vào **`http://localhost:9411`**.
2. Nhấp vào nút **"Run Query"** hoặc chọn lọc theo **Service Name** (ví dụ: `user-service`).
3. Nhấp vào một **Trace** cụ thể để mở sơ đồ thác nước (**Waterfall Diagram**):
   * Thanh ngang màu xanh/đỏ thể hiện thời gian tiêu tốn (Duration) tại từng microservice.
   * Nếu API bị chậm, bạn sẽ nhìn thấy ngay thanh ngang của service nào dài nhất (ví dụ: mất 500ms tại đoạn query Database).
4. **Lưu lại mã `TraceId`:** Ở góc trên bên trái của trang chi tiết, bạn sẽ thấy một chuỗi dài (ví dụ: `0a5421fcb903ecee699f293bcba5a119`). Hãy copy mã này để tra cứu log lỗi ở Kibana.

---

## 3. 📊 Hướng Dẫn Giám Sát Chỉ Số với Grafana & Prometheus (`:3001`)

Grafana giúp bạn biến những con số khô khan thành biểu đồ trực quan để phát hiện sớm nguy cơ sập server (OOM RAM, tràn CPU).

### Bước 1: Kết nối Data Source Prometheus
1. Đăng nhập vào Grafana tại **`http://localhost:3001`** (`admin`/`admin`).
2. Vào **Connections** ➔ **Data sources** ➔ **Add data source** ➔ Chọn **Prometheus**.
3. Tại ô **Prometheus server URL**, nhập chính xác: `http://prometheus:9090` *(lưu ý dùng tên container `prometheus` và cổng nội bộ `9090`)*.
4. Bấm **Save & test** ở dưới cùng. Nếu báo màu xanh *"Successfully queried the Prometheus API"* là thành công!

### Bước 2: Import Dashboard chuẩn Spring Boot 3 (JVM / Micrometer)
Không cần tự vẽ biểu đồ từ đầu, bạn có thể dùng sẵn Dashboard chuẩn của cộng đồng toàn cầu:
1. Nhấp vào dấu cộng **"+"** ở menu bên trái ➔ **Import dashboard**.
2. Tại ô **Import via grafana.com**, nhập mã ID: **`11378`** *(Khuyên dùng - JVM Micrometer chuẩn nhất cho Spring Boot 3)* rồi bấm **Load**.
3. **⚠️ BƯỚC QUYẾT ĐỊNH (Tránh lỗi Datasource not found):** Ở dưới cùng màn hình Options, tại ô dropdown **`prometheus`** (hoặc `Select a Prometheus data source`), hãy nhấp vào và chọn Data Source **`Prometheus`** bạn vừa tạo ở trên ➔ Sau đó mới bấm **Import**.
4. **Thành quả:** Bạn sẽ có ngay một bảng điều khiển siêu đẹp hiển thị:
   * **JVM Memory:** Lượng RAM Heap/Non-Heap đang sử dụng của từng microservice.
   * **CPU Usage:** % CPU server đang tiêu thụ.
   * **HTTP Requests:** Số lượng request trên giây (RPS) và tỷ lệ lỗi 4xx/5xx.

---

## 4. 📝 Hướng Dẫn Tra Cứu Nhật Ký Tập Trung với Kibana (`:5601`)

Thay vì phải mở terminal gõ `docker logs` cho 10 container khác nhau, toàn bộ log của ZChat đã được hội tụ về Kibana.

### Bước 1: Tạo Data View (Lần đầu truy cập)
1. Mở Kibana tại **`http://localhost:5601`**.
2. Vào menu bên trái ➔ **Stack Management** ➔ **Data Views** ➔ **Create data view**.
3. Tại ô **Name**, nhập: `ZChat Logs`.
4. Tại ô **Index pattern**, nhập: `zchat-logs*` *(Đây là chỉ mục mà Logstash đã cấu hình đẩy vào)*.
5. Chọn Timestamp field là `@timestamp` ➔ Bấm **Save data view**.

### Bước 2: Kỹ Thuật Điều Tra Sự Cố Thần Tốc (KQL Search)
Khi khách hàng báo lỗi hệ thống vào lúc 14:30, đây là cách bạn điều tra ra nguyên nhân trong 30 giây:
1. Vào menu **Discover** trên Kibana.
2. Lấy mã **`TraceId`** bạn copy được từ Zipkin hoặc Grafana (ví dụ `0a5421fcb903ecee699f293bcba5a119`).
3. Gõ vào ô tìm kiếm KQL của Kibana:
   ```kql
   traceId : "0a5421fcb903ecee699f293bcba5a119"
   ```
4. **Kết quả:** Kibana sẽ lọc ra **chính xác toàn bộ các dòng log** liên quan đến request đó, từ lúc bước vào `api-gateway`, đi qua `user-service`, cho đến câu lệnh SQL lỗi tại `storage-service`. Bạn sẽ nhìn thấy ngay dòng `ERROR NullPointerException` nằm ở file nào, dòng số mấy!

---

## 5. 🔄 Sa Bàn Quy Trình Xử Lý Sự Cố (Incident Response Workflow)

Để dễ hình dung, dưới đây là sơ đồ quy trình phối hợp 3 công cụ khi xảy ra sự cố trên môi trường Production:

```mermaid
sequenceDiagram
    autonumber
    actor DevOps as Quản trị viên (DevOps)
    participant G as Grafana Dashboard
    participant Z as Zipkin Tracing
    participant K as Kibana Log Analytics

    Note over DevOps, G: Phát hiện bất thường
    DevOps->>G: Nhận cảnh báo tỷ lệ lỗi HTTP 500 tăng vọt ở User Service
    G-->>DevOps: Xác định thời điểm xảy ra lỗi (VD: 10:15 AM)

    Note over DevOps, Z: Định vị điểm nghẽn
    DevOps->>Z: Lọc các request lỗi 500 lúc 10:15 AM của User Service
    Z-->>DevOps: Hiển thị sơ đồ thác nước bị lỗi tại endpoint /api/v1/users/update
    DevOps->>Z: Copy mã TraceId của request lỗi

    Note over DevOps, K: Đào sâu nguyên nhân gốc rễ (Root Cause)
    DevOps->>K: Dán TraceId vào ô tìm kiếm KQL
    K-->>DevOps: Hiển thị trọn bộ log chi tiết: Exception Stacktrace, SQL Query bị từ chối
    Note over DevOps: Bắt đúng bệnh ➔ Fix bug thần tốc!
```

---

## 6. 📲 Hướng Dẫn Tích Hợp Cảnh Báo Ra Telegram (Telegram Alerting via Grafana)

Trong kiến trúc ZChat, cách nhanh nhất và chuẩn nhất để gửi cảnh báo tự động ra Telegram là sử dụng tính năng **Grafana Alerting (Contact Points)** đã được tích hợp sẵn trên giao diện Grafana (cổng `3001`). Hệ thống sẽ tự động đọc 4 luật cảnh báo sinh tồn trong `alert.rules.yml` và nhắn tin báo động ngay khi xảy ra sự cố.

### Bước 1: Tạo Telegram Bot (Khâu chuẩn bị trên Telegram)
1. Mở ứng dụng Telegram, tìm kiếm bot **`@BotFather`** (Bot chính chủ của Telegram có tích xanh).
2. Gõ lệnh `/newbot` để tạo bot mới.
3. Nhập tên hiển thị cho Bot (Ví dụ: `ZChat Observability Alert`).
4. Nhập username cho Bot (bắt buộc kết thúc bằng chữ `_bot`, ví dụ: `zchat_monitor_bot`).
5. **@BotFather** sẽ gửi lại cho bạn một chuỗi mã xác thực gọi là **Bot API Token** (Có dạng: `7123456789:AAHdqTcvCH1vGWJxf-a...`). 
   👉 **Hãy copy và lưu mã Token này lại.**

### Bước 2: Lấy Chat ID của bạn (Hoặc Group DevOps)
Bot Telegram cần biết phải gửi tin nhắn cảnh báo cho ai (Cá nhân bạn hay Nhóm chat DevOps).
1. Trên Telegram, hãy tìm bot bạn vừa tạo (ví dụ `@zchat_monitor_bot`) và bấm **Start** (hoặc gửi một tin nhắn bất kỳ như `Hello` vào group có add bot đó).
2. Để lấy **Chat ID** nhanh nhất, hãy tìm và nhắn tin cho bot hỗ trợ **`@userinfobot`** (hoặc `@RawDataBot`), bot sẽ trả về cho bạn dòng **`Id: 123456789`** (Nếu là Group chat, ID thường có dấu trừ ở đầu, ví dụ `-100987654321`).
   👉 **Hãy copy mã Chat ID này lại.**

### Bước 3: Cấu hình trên Giao diện Grafana (`http://localhost:3001`)

#### 3.1. Tạo Contact Point (Điểm kết nối Telegram)
1. Đăng nhập vào Grafana tại **`http://localhost:3001`** (tài khoản: `admin` / `admin`).
2. Nhấp vào biểu tượng cái chuông 🔔 **Alerting** ở menu bên trái ➔ Chọn **Contact points**.
3. Bấm nút màu xanh **+ Add contact point**:
   * **Name**: Nhập `Telegram DevOps Team`.
   * **Integration**: Chọn **Telegram**.
   * **BOT API Token**: Dán mã **Bot Token** (lấy ở Bước 1) vào đây.
   * **Chat ID**: Dán mã **Chat ID** (lấy ở Bước 2) vào đây.
4. 🧪 **Kiểm tra kết nối**: Bấm vào nút **Test** ở góc trên bên phải ➔ Bấm **Send test notification**. Nếu bạn nhận được tin nhắn từ Bot trên điện thoại với nội dung *"Test alert from Grafana"* là kết nối thành công 100%!
5. Bấm **Save contact point** để lưu lại.

#### 3.2. Gắn vào Chính sách thông báo (Notification Policy)
1. Vẫn trong menu 🔔 **Alerting** bên trái ➔ Chọn **Notification policies**.
2. Tại mục **Default policy** (ở ngay đầu trang), nhấp vào nút **Edit** (hoặc biểu tượng cây bút ✏️).
3. Tại ô **Default contact point**, chọn giá trị từ dropdown là **`Telegram DevOps Team`** (mục bạn vừa tạo ở Bước 3.1).
4. Bấm **Update default policy** để hoàn tất.

### 🎉 Minh họa Tin nhắn Cảnh báo (Telegram Alert Message)
Khi bất kỳ microservice nào gặp sự cố (ví dụ: `user-service` bị sập, CPU/RAM vượt 85%, hoặc lỗi HTTP 5xx tăng vọt), Telegram của bạn sẽ nhận được thông báo đỏ 🚨:

```text
🔥 [FIRING: 1] InstanceDown (critical zchat-user-service)

🔴 CẢNH BÁO: Microservice zchat-user-service đã bị DOWN!
• Service: user-service (tại host.docker.internal:8010)
• Mô tả: Service không phản hồi Prometheus scrape trong 1 phút qua.
• Thời gian phát hiện: 2026-07-05 16:35:00
👉 Link truy cập Dashboard: http://localhost:3001/d/11378...
```

Khi sự cố được khắc phục, Grafana sẽ tự động gửi thông báo xanh 🟢 **[RESOLVED]** để xác nhận hệ thống đã phục hồi!
