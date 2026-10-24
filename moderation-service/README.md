# 🛡️ ZChat Moderation Service

Dịch vụ kiểm duyệt nội dung tin nhắn tự động thời gian thực (Real-time ML Content Moderation) cho hệ thống **ZChat**, xây dựng bằng **FastAPI**, **Apache Kafka** và mô hình học sâu **PhoBERT** (VinAI) được fine-tune trên tập dữ liệu **ViHSD** (Vietnamese Hate Speech Detection).

---

## 📋 Mục Lục
1. [Tổng Quan Kiến Trúc](#-tổng-quan-kiến-trúc)
2. [Cấu Trúc Thư Mục](#-cấu-trúc-thư-mục)
3. [Cài Đặt & Khởi Chạy](#-cài-đặt--khởi-chạy)
4. [Kiểm Thử Mô Hình (Testing)](#-kiểm-thử-mô-hình-testing)
5. [Các Giải Pháp Tối Ưu Tốc Độ (Performance Optimization)](#-các-giải-pháp-tối-ưu-tốc-độ-performance-optimization)
6. [Kafka Topics & API](#-kafka-topics--api)

---

## 🏛️ Tổng Quan Kiến Trúc

Dịch vụ hoạt động theo mô hình **Post-moderation bất đồng bộ (Asynchronous Event-driven)**:

```
[Chat Client] ──(WS: Send Message)──► [chat-service]
                                            │
                                  Lưu DB & Publish event
                                            ▼
                                   [Kafka: chat-events]
                                            │
                                         Consume
                                            ▼
                           [moderation-service (PhoBERT)]
                                            │
                                  Phát hiện vi phạm
                                            ▼
                               [Kafka: moderation-events]
                                       │        │
                     ┌─────────────────┘        └─────────────────┐
                     ▼                                            ▼
       [chat-service (Consumer)]                    [notification-service]
     - Cập nhật state = FLAGGED                   - Gửi thông báo cảnh báo
     - Bắn WS làm mờ tin nhắn                       đến người gửi
```

---

## 📂 Cấu Trúc Thư Mục

```
moderation-service/
├── app/
│   ├── api/                  # API routes (health check, etc.)
│   ├── config.py             # Cấu hình Pydantic (Kafka, thresholds, model path)
│   ├── kafka/
│   │   ├── consumer.py       # Lắng nghe topic `chat-events`
│   │   └── producer.py       # Phát kết quả ra `moderation-events`
│   ├── main.py               # Điểm khởi chạy FastAPI + Lifespan
│   ├── ml/
│   │   └── classifier.py     # Class nạp PhoBERT & thực hiện suy luận (Inference)
│   └── models/
│       └── schemas.py        # Pydantic schemas (ChatEvent, ModerationResult)
├── model/                    # Thư mục chứa trọng số PhoBERT đã train
│   ├── config.json
│   ├── model.safetensors     # File weights chính (~540MB)
│   ├── tokenizer_config.json
│   └── vocab.txt
├── scripts/
│   └── test_model.py         # Script kiểm thử nhanh mô hình trực tiếp
├── .venv/                    # Virtual Environment Python
├── Dockerfile                # Cấu hình container chạy Docker
├── requirements.txt          # Danh sách thư viện Python cần thiết
└── README.md
```

---

## ⚙️ Cài Đặt & Khởi Chạy

### Cách 1: Chạy trực tiếp bằng Python (Khuyên dùng khi Dev)

1. **Kích hoạt Virtualenv hoặc tạo mới:**
   ```powershell
   cd D:\dev\projects\zchat\moderation-service
   .\.venv\Scripts\activate
   ```

2. **Cài đặt thư viện:**
   ```powershell
   pip install -r requirements.txt
   ```

3. **Khởi chạy service:**
   ```powershell
   uvicorn app.main:app --host 0.0.0.0 --port 8060 --reload
   ```
   Service sẽ mở tại: `http://localhost:8060` (Healthcheck: `http://localhost:8060/health`).

---

### Cách 2: Chạy bằng Docker Compose

Dịch vụ đã được cấu hình sẵn trong file `docker-compose.yml` gốc của dự án:
```powershell
docker compose up -d moderation-service
```

---

## 🧪 Kiểm Thử Mô Hình (Testing)

Bạn có thể kiểm tra trực tiếp độ nhạy và tính năng nhận diện của PhoBERT bằng script có sẵn:

```powershell
.\.venv\Scripts\python.exe scripts/test_model.py
```

Kết quả mẫu:
* **"Chào bạn, hôm nay bạn thế nào?"** ➔ `CLEAN` (99.7%)
* **"Mày ngu như bò vậy, biến đi cho khuất mắt."** ➔ `HATE` (97.4%)
* **"Đồ rác rưởi hạ đẳng, cút đi."** ➔ `HATE` (96.6%)

---

## ⚡ Các Giải Pháp Tối Ưu Tốc Độ (Performance Optimization)

Mô hình PhoBERT có kích thước ~135 triệu tham số. Mặc định trên CPU, thời gian suy luận dao động từ **100ms - 250ms/câu**. Dưới đây là các kỹ thuật giúp tăng tốc từ **x2 đến x10 lần**:

### 1. 🚀 Giảm `max_length` của Tokenizer (Tăng tốc x2 - x3 lần)
* **Vấn đề:** Mặc định `max_length = 256`. Tính toán cơ chế Attention trong Transformer có độ phức tạp $O(N^2)$ theo độ dài chuỗi token.
* **Tối ưu:** Tin nhắn trò chuyện thực tế hầu như không vượt quá 30 - 40 từ.
* **Cách làm:** Trong `app/ml/classifier.py`, giảm `max_length=256` xuống **`max_length=64`** hoặc **`max_length=96`**.
* **Hiệu quả:** Cắt giảm ngay 50% - 60% thời gian tính ma trận Attention mà không làm ảnh hưởng độ chính xác.

---

### 2. ⚡ In-Memory LRU Cache (Tốc độ trả về tức thì: 0.001ms)
* **Vấn đề:** 30% - 50% tin nhắn hàng ngày là các câu thông dụng lặp đi lặp lại: *"ok", "alo", "haha", "cảm ơn", "chào bạn", "ừ", "bye"...*
* **Cách làm:** Sử dụng `@functools.lru_cache(maxsize=10000)` hoặc cache Dictionary trong bộ nhớ RAM cho phương thức `predict(text)`.
* **Hiệu quả:** Đối với các câu chat quen thuộc, kết quả trả về trong chưa đầy **0.001ms**, không cần tốn tài nguyên chạy mô hình Transformer.

---

### 3. 🛡️ Bộ Lọc Nhanh Fast-Path (Quy tắc lọc sơ bộ)
* **Quy tắc:**
  * **Chỉ chứa Emoji:** Các tin nhắn chỉ gồm emoji (`👍`, `❤️`, `😂`,...) ➔ Bỏ qua hoặc gán nhãn `CLEAN` ngay lập tức.
  * **Đường dẫn URL / File / Ảnh:** Không chứa văn bản tự do ➔ Gán `CLEAN` ngay.
  * **Whitelist từ đơn an toàn:** Những từ thông dụng xác định an toàn ➔ Trả về `CLEAN`.
* **Hiệu quả:** Giảm tải 30% - 40% lượng request phải chuyển vào mô hình PhoBERT.

---

### 4. 💎 Lượng Tử Hóa ONNX Runtime INT8 (Chuẩn Production, tăng tốc x3 - x4 lần trên CPU)
* **Cách làm:**
  1. Chuyển đổi mô hình PyTorch sang định dạng **ONNX** (`torch.onnx.export`).
  2. Áp dụng lượng tử hóa động (**Dynamic Quantization**): Nén trọng số từ kiểu số thực 32-bit (`FP32`) xuống số nguyên 8-bit (`INT8`).
  3. Dùng thư viện `onnxruntime` để chạy suy luận.
* **Hiệu quả:**
  * Dung lượng mô hình giảm từ **540MB ➔ ~135MB** (tiết kiệm 75% RAM).
  * Tốc độ suy luận CPU giảm từ **150ms ➔ ~30ms - 40ms/tin nhắn**.

---

### 5. 🎮 Sử dụng Card Đồ Họa Rời (NVIDIA GPU / CUDA)
* Nếu máy chủ / máy tính có trang bị card màn hình NVIDIA (GTX / RTX / Tesla):
  * Cài đặt PyTorch phiên bản hỗ trợ CUDA:
    ```powershell
    pip install torch --index-url https://download.pytorch.org/whl/cu121
    ```
  * Class `ToxicityClassifier` đã được viết sẵn tự động phát hiện GPU (`self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")`).
* **Hiệu quả:** Tốc độ suy luận đạt mức cực đại: chỉ **5ms - 10ms/tin nhắn**.

---

### 6. 📦 Xử Lý Theo Lô (Batching Inference)
* Khi hệ thống có tải cao (nhiều người nhắn tin cùng lúc):
  * Thay vì suy luận từng tin nhắn lẻ `batch_size = 1`, gom các tin nhắn đến từ Kafka trong khoảng thời gian 20ms - 50ms thành lô (`batch_size = 8` hoặc `16`).
  * PyTorch xử lý lô ma trận song song nhanh gấp 3 - 5 lần so với chạy 16 lần đơn lẻ.

---

## 📡 Kafka Topics & API

| Thành phần | Tên | Kiểu | Mô tả |
|------------|-----|------|-------|
| **Input Topic** | `chat-events` | Kafka Consumer | Nhận tin nhắn mới từ `chat-service` (`type: NEW_MESSAGE`, `recipientId: null`) |
| **Output Topic** | `moderation-events` | Kafka Producer | Bắn kết quả kiểm duyệt (`action: FLAGGED` hoặc `HIDDEN`) |
| **Health API** | `GET /health` | REST HTTP | Trả về trạng thái hoạt động và cờ `model_loaded: true/false` |
| **Gateway Route** | `/api/v1/moderation/**` | Spring Cloud Gateway | Route chuyển tiếp tới port 8060 |
