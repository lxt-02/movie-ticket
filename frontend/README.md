# MTK Cinemas - Movie Ticket Booking Frontend

Hệ thống giao diện Web đặt vé xem phim hiện đại tiêu chuẩn quốc tế, kết nối kiến trúc Microservices (Spring Boot, Keycloak, Kafka, Postgres, Redis, PayOS).

## 🎬 Tính năng chính

- **Trang chủ MTK Cinemas:** Hero Carousel bom tấn, công nghệ phòng chiếu (IMAX Laser, Dolby Atmos, ScreenX, SweetBox).
- **Phim đang chiếu (`/phim-dang-chieu`):** Lọc theo thể loại, tìm kiếm theo tên phim/diễn viên, phân loại độ tuổi điện ảnh (`P`, `C13`, `C16`, `C18`).
- **Chọn rạp & Suất chiếu:** Danh sách cụm rạp tiêu biểu, lịch chiếu theo ngày và phòng chiếu.
- **Sơ đồ ghế thông minh (Real-time Seat Matrix):** Chọn ghế Thường, VIP, Ghế đôi SweetBox với cơ chế giữ chỗ 10 phút qua Redis.
- **Thanh toán trực tuyến PayOS:** Quét mã VietQR thanh toán tức thì và in vé điện tử (E-Ticket) với mã QR check-in.
- **Quản lý vé của tôi:** Lịch sử đơn đặt vé và mã vé điện tử.
- **Quầy Vận Hành & Soát Vé (`STAFF`):** Giao diện quét/soát vé tại cửa phòng chiếu và giám sát hàng đợi Outbox/Kafka DLT.
- **Trang Quản Trị Hệ Thống (`ADMIN`):** Quản lý người dùng và khóa/mở khóa tài khoản `ACTIVE` / `BLOCKED`.
- **Trang thông tin chuyên biệt:** Giới thiệu rạp MTK, Đặc trưng công nghệ, Điều khoản chung, Điều khoản giao dịch, Chính sách thanh toán, Chính sách bảo mật, FAQ.

## 🛠️ Công nghệ sử dụng

- **Core:** React 18, Vite
- **Routing:** React Router DOM
- **Styling:** Tailwind CSS, PostCSS
- **Icons:** Lucide React
- **Auth & API:** Keycloak OIDC JWT, Fetch API Gateway (`http://localhost:8080`)

## 🚀 Khởi chạy dự án

```bash
# Cài đặt thư viện
npm install

# Khởi chạy máy chủ phát triển (Port 3000)
npm run dev

# Đóng gói sản xuất
npm run build
```
