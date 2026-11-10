# MSS301 – Frontend

React 18 + Vite + TypeScript + React Router + keycloak-js. Gọi **toàn bộ API qua API Gateway** (`http://localhost:8080`),
đăng nhập bằng Keycloak (Authorization Code + PKCE S256). Hợp đồng API: `FRONTEND_API.md`.

## Chạy

```bash
# 1. Backend: ở thư mục gốc
docker compose up -d
# 2. Frontend
cd frontend
cp .env.example .env      # nếu chưa có
npm install
npm run dev               # http://localhost:3000  (bắt buộc port 3000: Keycloak redirect, CORS, PayOS return URL)
```

Tài khoản demo (mật khẩu `123456`): `customer`, `staff`, `admin`, `blocked`.

## Role → trang → API

| Role | Trang | API dùng |
|---|---|---|
| Khách (chưa đăng nhập) | `/` phim · `/search` tìm kiếm · `/movies/:id` chi tiết + lịch chiếu · `/cinemas`, `/cinemas/:id` · `/showtimes/:id` sơ đồ ghế | `GET /movies`, `/search/movies`, `/search/movies/suggest`, `/cinemas`, `/cinemas/{id}/screens`, `/showtimes`, `/showtimes/{id}/seats` |
| Đã đăng nhập (CUSTOMER, cả STAFF/ADMIN) | Đặt vé trên `/showtimes/:id` · `/checkout/:bookingId` · `/payment/success`, `/payment/cancel` · `/my-bookings`, `/my-bookings/:id` · `/profile` | `GET/PUT /users/me`, `POST /bookings` (+`Idempotency-Key`), `GET /bookings`, `/bookings/{id}`, `/bookings/code/{code}`, `POST /bookings/{id}/cancel`, `/payment-retry`, `GET /payments/by-booking/{id}`, `POST /payments/by-booking/{id}/sync` |
| STAFF (+ ADMIN) | `/ops` vận hành | `/bookings/admin/payment-events-dlt[/replay|/stop]`, `/bookings/admin/outbox[/requeue-failed]`, `/payments/admin/outbox[/requeue-failed]`, `/bookings/admin/chaos/payment-events` |
| ADMIN | `/admin` · `/admin/movies` · `/admin/catalog` · `/admin/cinemas` · `/admin/cinemas/:id` (phòng) · `/admin/screens/:id` (sơ đồ ghế) · `/admin/showtimes` · `/admin/users` | Toàn bộ API 🛠 trong `FRONTEND_API.md` §4 |

Guard ở FE (`src/auth/RequireRole.tsx`) chỉ ẩn trang; backend vẫn kiểm quyền ở mọi request.

## Luồng mua vé (đã cài theo FRONTEND_API §3)

1. Sau đăng nhập gọi `GET /users/me` **một lần** (`AuthContext`).
2. Sơ đồ ghế: chỉ chọn `AVAILABLE`, tối đa 8 ghế, tự làm mới mỗi 20 s và khi gặp `409`; ẩn nút đặt vé khi còn < 15 phút.
3. `POST /bookings`: key mới cho mỗi bộ ghế; `503/504/mất mạng` → gửi lại **cùng key**; `400/409` → bỏ key; `429` → chờ `Retry-After`.
4. Checkout: đếm ngược theo `expiresAt`; hỏi `GET /payments/by-booking/{id}` mỗi 1,5 s (≤ 30 s, `404` là bình thường);
   lưu `bookingId` vào `sessionStorage` rồi chuyển sang `checkoutUrl` của PayOS.
5. Quay về `/payment/success|cancel`: **không tin URL**, gọi `POST .../sync` rồi hỏi booking mỗi 2 s (≤ 60 s),
   xét cả `payment.status` lẫn `booking.status` (`src/utils/paymentFlow.ts → decide`).
6. Thanh toán lại: `POST /payment-retry` → chờ payment có `id` khác (sau 10 s dùng link cũ nếu backend giữ lại).

## Cấu trúc

```
src/
  api/        client.ts (fetch + token + lỗi chuẩn), endpoints.ts (mọi API), types.ts
  auth/       keycloak.ts, AuthContext.tsx, RequireRole.tsx
  components/ Layout, ui (badge, lỗi, phân trang, modal, hooks), MovieCard, SuggestBox, ShowtimeBrowser, Countdown, TicketList, RowsEditor
  pages/      trang khách + CUSTOMER; staff/OpsPage; admin/*
  utils/      format.ts (giờ VN, tiền), paymentFlow.ts
```
