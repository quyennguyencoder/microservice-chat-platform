# Frontend Deep-Dive: React 19, Gestalt UI, TanStack Query & Zustand

Tài liệu này mô tả chi tiết kiến trúc Frontend (`zchat-client`), lý do lựa chọn bộ công cụ (`Tech Stack Selection`), cách quản lý trạng thái (`State Management`), và các kỹ thuật tối ưu hóa hiệu năng giao diện cho ứng dụng nhắn tin thời gian thực ZChat.

---

## 1. Tại sao lựa chọn Pinterest Gestalt UI Design System?

Trong khi đa số các ứng dụng sinh viên hay thực tập sinh đều sử dụng TailwindCSS, Ant Design hoặc Material-UI (MUI), ZChat quyết định áp dụng **Pinterest Gestalt (`@gestalt/design-system`)** làm hệ thống thiết kế cốt lõi.

### Lý do Kiến trúc & Trải nghiệm Người dùng (`UX Rationale`):
* **Tiêu chuẩn Hàng đầu của Tổ chức lớn (`Enterprise Standardization`):** Gestalt là Design System được chính đội ngũ kỹ sư của Pinterest phát triển và sử dụng cho hàng trăm triệu người dùng trên toàn cầu. Các component (`Box`, `Flex`, `Modal`, `TextField`, `Spinner`) đều được thiết kế tối ưu tuyệt đối về bố cục và độ tương phản.
* **Tuân thủ Tuyệt đối Tiêu chuẩn Trợ năng (`Strict Accessibility - a11y`):** Tất cả các component trong Gestalt đều bắt buộc phải có nhãn hỗ trợ người khuyết tật (`accessibilityLabel`). Khi dùng Gestalt, giao diện tự động đạt chuẩn WCAG 2.1 AA mà không cần viết thêm CSS phức tạp.
* **Kiểm soát Hệ thống Mã màu & Layout (`Token-driven Design`):** Tránh hiện tượng "CSS Rác" (`CSS Clutter`). Lập trình viên không thể tùy tiện gõ các màu nháy hoặc margin/padding tùy tiện; mọi khoảng cách (`margin`, `padding`) đều tuân theo lưới chuẩn 4px (`size={1}` $\rightarrow$ 4px, `size={2}` -> 8px...), giúp tổng thể UI cực kỳ cân đối và chuyên nghiệp như một sản phẩm thương mại thực thụ.

---

## 2. Quản lý Trạng thái (`State Management`) Phân lớp Chuẩn mực

Thay vì nhồi nhét toàn bộ trạng thái vào Redux gây phình to và chậm chạp, ZChat áp dụng mô hình **Phân chia Trách nhiệm Trạng thái (`Separation of State Responsibilities`)** bằng cách kết hợp 2 công cụ mạnh mẽ nhất hiện nay: **TanStack React Query** + **Zustand**.

```
+-----------------------------------------------------------------------------------+
|                              ZCHAT FRONTEND ARCHITECTURE                          |
+-----------------------------------------+-----------------------------------------+
|     SERVER STATE (TanStack Query)       |       CLIENT STATE (Zustand Stores)     |
+-----------------------------------------+-----------------------------------------+
| - Danh sách bạn bè (`/users/friends`)   | - Trạng thái Auth (`accessToken`, User) |
| - Lịch sử tin nhắn (`/chats/{id}/msgs`) | - Phòng chat đang mở (`activeChatId`)   |
| - Kết quả tìm kiếm người dùng           | - Trạng thái gõ phím (`typingUsers`)    |
| - Cache, Stale Time, Background Refetch | - Giao diện Modal/Sidebar isOpen        |
+-----------------------------------------+-----------------------------------------+
```

### A. Quản lý Trạng thái Máy chủ (`Server State`) với TanStack React Query (`useQuery` / `useMutation`)
* **Vấn đề:** Khi fetch dữ liệu từ API (`/api/v1/users/...`), nếu lưu vào state React thông thường, mỗi lần chuyển trang lại phải gọi API lại từ đầu, gây giật lag và tốn băng thông.
* **Giải pháp với TanStack Query:**
  * **Tự động Lưu đệm (`Smart Caching & Deduplication`):** Khi người dùng mở phòng chat A, lịch sử tin nhắn được cache. Nếu họ sang phòng B rồi quay lại phòng A ngay, dữ liệu hiển thị tức thì từ Cache trong lúc React Query ngầm fetch kiểm tra tin nhắn mới (`Stale-While-Revalidate`).
  * **Đã tối ưu hóa Production:** Bộ công cụ kiểm thử `ReactQueryDevtools` đã được loại bỏ hoàn toàn khỏi `QueryProvider.tsx` trước khi đóng gói (`npm run build`) để đảm bảo giao diện sạch bóng, không để lộ thông tin debug ra ngoài.

### B. Quản lý Trạng thái Máy khách (`Client State`) với Zustand (`authStore`, `chatStore`)
* **Tại sao không dùng Redux Toolkit?** Redux đòi hỏi rất nhiều boilerplate code (Action, Reducer, Slice, Provider wrapper), nặng nề cho một SPA.
* **Tại sao chọn Zustand?**
  * Siêu nhẹ (chỉ ~1KB), cú pháp cực kỳ gọn gàng (`create((set) => (...))`).
  * Không cần bọc `<Provider>` quanh root App, có thể gọi trực tiếp bên ngoài React Component (ví dụ trong các Axios Interceptor hoặc WebSocket Service).
  * **Zustand Stores trong ZChat:**
    * `authStore`: Lưu trữ `accessToken`, thông tin `user` hiện tại, xử lý lưu persist xuống `localStorage` hoặc `sessionStorage`.
    * `chatStore`: Lưu trữ phòng chat đang chọn (`selectedChatId`), danh sách những người đang online, và danh sách những người đang gõ phím (`typingUsers`).

---

## 3. Các Kỹ thuật Tối ưu Hóa Hiệu năng Giao diện (`UI Performance Optimizations`)

### A. Tải chậm & Chia nhỏ Mã nguồn (`Code Splitting & Lazy Loading`)
Vị trí file: `zchat-client/src/app/router/LazyRoutes.tsx`

Khi người dùng lần đầu vào web, họ chỉ cần tải trang Đăng nhập (`LoginPage`) hoặc trang chính (`MessagesPage`). Việc bắt họ tải luôn cả code của trang Cài đặt (`SettingsPage`), trang Tìm kiếm (`SearchPage`), hay trang Bạn bè (`FriendsPage`) là cực kỳ lãng phí.
* **Cách ZChat triển khai:** Sử dụng `React.lazy()` kết hợp `<Suspense fallback={<FullPageLoader />}>` trong `LazyRoutes.tsx`.
* **Hiệu quả:** Webpack/Vite sẽ tự động cắt nhỏ JS bundle thành các file `.chunk.js` riêng biệt. Khi bấm vào menu "Cài đặt", trình duyệt mới bắt đầu tải file chunk của `SettingsPage`, giúp thời gian tải trang ban đầu (`FCP - First Contentful Paint`) cực kỳ nhanh chóng.

---

### B. Tối ưu Hiệu ứng Gõ phím (`Typing Indicator Throttling & Debouncing`)
Vị trí file: `zchat-client/src/modules/chat/components/MessageInput.tsx` & `useChatWebSocket.ts`

Như đã trình bày ở phần WebSocket, Frontend sử dụng 2 chốt chặn logic ngay tại ô nhập tin nhắn:
1. **Throttling (`2.5 giây`):** Giới hạn tần suất gửi lệnh `TYPING_START` qua WebSocket tối đa 1 lần mỗi 2.5 giây.
2. **Debouncing (`2 giây`):** Sau 2 giây không có thao tác bàn phím, tự động gửi `TYPING_STOP`.
3. **Safety Timeout (`5 giây` tại `useChatWebSocket.ts`):** Tự động dọn dẹp animation gõ phím nếu bị mất gói tin `TYPING_STOP` từ đối phương.

---

## 4. Câu hỏi Phỏng vấn & Phương án Bảo vệ Kiến trúc

### ❓ Câu hỏi 1: *"Nhà tuyển dụng: Giao diện web của em nhìn rất đẹp và chuyên nghiệp, giống hệt Pinterest hay Zalo. Có phải em lấy template/source code của ai khác không?"*
**Cấu trúc trả lời:**
> *"Dạ không ạ! Em tự tay xây dựng toàn bộ Frontend từ con số 0 dựa trên **Pinterest Gestalt UI Design System (`@gestalt/design-system`)**.
> Lý do giao diện của em nhìn cực kỳ chuyên nghiệp và chuẩn mực là bởi vì thay vì tự viết CSS ad-hoc hay xài Tailwind tùy tiện dễ bị lệch margin/padding, em tuân thủ nghiêm ngặt hệ thống **Design Tokens và Flexbox Layout chuẩn hóa (`Box`, `Flex`, `TextField`)** của chính kỹ sư Pinterest. Các khoảng cách luôn đi theo lưới bội số của 4px, tỷ lệ tương phản màu sắc đạt chuẩn Accessibility AA, và các trạng thái Loading/Spinner đều được chăm chút kỹ lưỡng để mang lại trải nghiệm thương mại thực thụ!"*

### ❓ Câu hỏi 2: *"Tại sao em lại phải chia ra TanStack Query cho Server State và Zustand cho Client State mà không lưu tất cả vào Zustand cho tiện?"*
**Cấu trúc trả lời:**
> *"Dạ, việc phân tách Server State và Client State là một trong những best-practice quan trọng nhất của Modern Frontend Engineering:
> - **Server State (Dữ liệu từ API như tin nhắn, bạn bè):** Có đặc thù là bất đồng bộ, có thể bị lỗi thời (`stale`), cần cơ chế tự động thử lại (`retry`), cache, deduplicate requests và background refetching. Nếu tự viết các cơ chế này trong Zustand hay Redux thì sẽ tốn hàng nghìn dòng code phức tạp. TanStack Query giải quyết trọn vẹn 100% các vấn đề này chỉ với 1 hook `useQuery`.
> - **Client State (Dữ liệu UI thuần túy như Modal mở/tắt, Active Chat, Auth Token):** Là dữ liệu đồng bộ, chỉ tồn tại trên trình duyệt và cần truy xuất siêu nhanh ở nhiều component. Dùng **Zustand** giúp em quản lý phần này cực kỳ nhẹ nhàng mà không bị re-render dư thừa."*
