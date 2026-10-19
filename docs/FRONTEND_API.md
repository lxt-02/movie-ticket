# MSS301 Movie Booking – API cho Frontend

> Tài liệu này mô tả **các API đã có trong backend** (đọc trực tiếp từ code), dùng làm đặc tả để code Frontend.
> Mọi request đi qua **API Gateway**. Không gọi thẳng từng service.

---

## 0. Thông tin chung

### 0.1 Base URL

| Môi trường | Base URL |
|---|---|
| Gateway (local) | `http://localhost:8080` |
| Frontend (dev) | `http://localhost:3000` (PayOS redirect về đây, xem mục 5) |

**CORS:** Gateway **đã bật CORS** cho origin `http://localhost:3000` (cấu hình qua biến `GATEWAY_CORS_ALLOWED_ORIGINS`, nhiều origin cách nhau bằng dấu phẩy).

| Cấu hình CORS | Giá trị |
|---|---|
| Allowed origins | `http://localhost:3000` |
| Allowed methods | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
| Allowed headers | `Content-Type, Authorization, X-User-Id, Idempotency-Key, X-Api-Key` |
| Exposed headers (JS đọc được) | `Retry-After, X-RateLimit-Remaining, X-RateLimit-Burst-Capacity, X-RateLimit-Replenish-Rate, X-RateLimit-Requested-Tokens, X-Served-By` |
| Credentials | Không (không dùng cookie; danh tính gửi qua header `X-User-Id`) |

Frontend (Next.js) có thể dùng **một trong hai cách**:

1. **Gọi thẳng Gateway** (nhờ CORS): `fetch('http://localhost:8080/api/v1/bookings', ...)`. Nên đặt base URL qua biến môi trường, ví dụ `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`.
2. **Qua Next.js rewrites** (cùng origin, không cần CORS), đã cấu hình trong `next.config.ts`:

```ts
// next.config.ts
async rewrites() {
  return [
    { source: '/api/:path*',        destination: 'http://localhost:8080/api/:path*' },
    { source: '/internal/:path*',   destination: 'http://localhost:8080/internal/:path*' },
    { source: '/rate-limit/:path*', destination: 'http://localhost:8080/rate-limit/:path*' },
  ];
}
```

Khi dùng rewrites thì để base URL rỗng và gọi đường dẫn tương đối: `fetch('/api/v1/bookings')`.

> Lưu ý: không đặt trang Next.js nào trùng prefix `/api/...` (App Router route handlers `app/api/**`), vì rewrites sẽ không chuyển những đường dẫn đó sang Gateway.

### 0.2 Header

| Header | Bắt buộc | Mô tả |
|---|---|---|
| `X-User-Id` | Có, với mọi API `/api/v1/bookings/**` và `/api/v1/payments/**` | UUID của user đang đăng nhập. **Chưa có login/JWT**, FE tạm lưu userId (ví dụ chọn user demo) và gửi header này. Thiếu header → `401`. |
| `Idempotency-Key` | Có, với `POST /api/v1/bookings` | Chuỗi ngẫu nhiên (khuyên dùng `crypto.randomUUID()`), sinh **một lần cho mỗi lần bấm "Đặt vé"**. Nếu request lỗi mạng và gửi lại, **dùng lại cùng key** để không tạo trùng booking. |
| `X-User-Role` | Có, với API **ghi** của movie / cinema / showtime (POST, PUT, PATCH, DELETE) | Giá trị `ADMIN` cho trang quản trị. Thiếu hoặc khác `ADMIN` → `403`. Với GET: gửi `ADMIN` để thấy cả phim `ARCHIVED`, rạp `CLOSED`, suất đã huỷ / đã chiếu. **Tạm thời cho dev**, sau này lấy từ JWT. |
| `Content-Type: application/json` | Với request có body | |

User demo có sẵn (xem mục 3): `10000000-0000-0000-0000-000000000001`.

### 0.3 Kiểu dữ liệu

| Kiểu Java | Kiểu JSON | Ví dụ |
|---|---|---|
| `UUID` | string | `"50000000-0000-0000-0000-000000000001"` |
| `Instant` | string ISO-8601, UTC | `"2030-01-15T12:00:00Z"` → FE tự đổi sang giờ VN (UTC+7) khi hiển thị |
| `LocalDate` | string `YYYY-MM-DD` | `"2024-03-01"` |
| `BigDecimal` | number | `250000.00` (đơn vị VND) |
| `enum` | string | `"PENDING"` (danh sách ở mục 2) |

Field có giá trị `null` vẫn có thể xuất hiện trong response (FE phải xử lý `null`).

### 0.4 Response thành công (envelope chung)

Mọi API thành công đều bọc dữ liệu trong:

```json
{
  "success": true,
  "message": "Success",
  "data": { },
  "timestamp": "2026-10-19T03:00:00.123Z"
}
```

FE luôn đọc dữ liệu ở `response.data`.

### 0.5 Response lỗi

```json
{
  "success": false,
  "status": 409,
  "error": "Conflict",
  "message": "Selected seats are no longer available",
  "path": "/api/v1/bookings",
  "timestamp": "2026-10-19T03:00:00.123Z"
}
```

Lỗi validate body (`400`) có thêm `errors` theo từng field:

```json
{
  "success": false,
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request payload",
  "path": "/api/v1/bookings",
  "errors": { "showtimeId": "showtimeId must not be null" },
  "timestamp": "2026-10-19T03:00:00.123Z"
}
```

FE hiển thị `message` cho người dùng; nếu có `errors` thì hiển thị dưới từng ô nhập.

| HTTP | Ý nghĩa chung |
|---|---|
| 200 / 201 / 202 | Thành công |
| 400 | Dữ liệu sai, trạng thái không cho phép, hoặc không có quyền với booking |
| 401 | Thiếu `X-User-Id` |
| 404 | Không tìm thấy (hoặc không phải của user này) |
| 409 | Xung đột (ghế đã bị người khác giữ, trùng Idempotency-Key với body khác) |
| 429 | Gửi quá nhanh (rate limit, chỉ các route demo ở mục 4.6) |
| 503 | Service phụ thuộc đang lỗi, thử lại sau |
| 500 | Lỗi hệ thống |

---

## 1. Danh sách endpoint

| # | Method | Path | Mô tả |
|---|---|---|---|
| 4.7.1 | GET | `/api/v1/movies` | **Danh sách phim** (lọc, tìm kiếm, phân trang) |
| 4.7.1 | GET | `/api/v1/movies/{id}` | Chi tiết phim |
| 4.7.1 | POST / PUT / PATCH / DELETE | `/api/v1/movies...` | Quản lý phim (admin) |
| 4.7.2 | GET | `/api/v1/cinemas` | **Danh sách rạp** |
| 4.7.2 | GET | `/api/v1/cinemas/{id}` · `/api/v1/cinemas/{id}/screens` | Chi tiết rạp · phòng chiếu của rạp |
| 4.7.2 | POST / PUT / PATCH / DELETE | `/api/v1/cinemas...` | Quản lý rạp (admin) |
| 4.7.3 | GET | `/api/v1/showtimes` | **Lịch chiếu** theo phim / rạp / phòng / ngày |
| 4.7.3 | GET | `/api/v1/showtimes/{id}` · `/api/v1/showtimes/{id}/seats` | Chi tiết suất · **sơ đồ ghế theo suất** (có `showtimeSeatId`, giá, trạng thái) |
| 4.3.1 | POST | `/api/v1/showtimes` | Tạo suất chiếu (admin, nay **cần `X-User-Role: ADMIN`**) |
| 4.7.3 | PUT / DELETE | `/api/v1/showtimes/{id}` | Đổi giờ / giá, huỷ suất (admin) |
| 4.1.1 | GET | `/internal/movies/{id}` | *(deprecated, dùng 4.7.1)* Chi tiết phim |
| 4.2.1 | GET | `/internal/cinemas/{id}` | *(deprecated, dùng 4.7.2)* Chi tiết rạp |
| 4.2.2 | GET | `/internal/screens/{id}` | *(deprecated)* Chi tiết phòng chiếu |
| 4.2.3 | GET | `/internal/screens/{id}/layout` | *(deprecated, dùng `/api/v1/showtimes/{id}/seats`)* Sơ đồ ghế vật lý |
| 4.4.1 | POST | `/api/v1/bookings` | Đặt vé (giữ ghế + tạo booking) |
| 4.4.2 | GET | `/api/v1/bookings` | Danh sách booking của tôi |
| 4.4.3 | GET | `/api/v1/bookings/{id}` | Chi tiết booking |
| 4.4.4 | GET | `/api/v1/bookings/code/{bookingCode}` | Tra booking theo mã |
| 4.4.5 | POST | `/api/v1/bookings/{id}/cancel` | Huỷ booking |
| 4.4.6 | POST | `/api/v1/bookings/{id}/payment-retry` | Thanh toán lại (tạo link PayOS mới) |
| 4.5.1 | GET | `/api/v1/payments/by-booking/{bookingId}` | Payment mới nhất của booking (lấy link PayOS) |
| 4.5.2 | GET | `/api/v1/payments/{id}` | Chi tiết payment |
| 4.5.3 | POST | `/api/v1/payments/by-booking/{bookingId}/sync` | Hỏi PayOS và cập nhật payment mới nhất |
| 4.5.4 | POST | `/api/v1/payments/{id}/sync` | Hỏi PayOS và cập nhật một payment |
| 4.6.x | GET | `/rate-limit/{user\|ip\|api-key}/ping` | Demo rate limit (không phải nghiệp vụ) |

> `/internal/movies`, `/internal/cinemas`, `/internal/screens` là API nội bộ, Gateway còn cho **GET** tạm thời để FE cũ
> không hỏng. **FE mới hãy dùng API public ở mục 4.7**; các route `/internal` sẽ bị bỏ khỏi Gateway.

---

## 2. Enum

| Enum | Giá trị | Ghi chú cho FE |
|---|---|---|
| `BookingStatus` | `PENDING`, `CONFIRMING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `REFUND_PENDING`, `REFUNDED` | `PENDING` = chờ thanh toán; `CONFIRMING` = đã trả tiền, đang xác nhận ghế; `CONFIRMED` = có vé |
| `BookingSeatStatus` | `ACTIVE`, `CANCELLED`, `EXPIRED`, `REFUNDED` | |
| `PaymentStatus` | `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `EXPIRED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED` | `PENDING`/`PROCESSING` = chưa kết thúc, các giá trị khác là trạng thái cuối |
| `ShowtimeStatus` | `SCHEDULED`, `SELLING`, `CANCELLED`, `ENDED` | |
| `MovieStatus` | `COMING_SOON`, `NOW_SHOWING`, `ENDED`, `ARCHIVED` | |
| `CinemaStatus` | `ACTIVE`, `INACTIVE`, `CLOSED` | |
| `ScreenStatus` | `ACTIVE`, `MAINTENANCE`, `INACTIVE` | |
| `SeatStatus` (ghế vật lý) | `ACTIVE`, `BROKEN`, `INACTIVE` | |
| `UserRole` | `CUSTOMER`, `STAFF`, `ADMIN` | |
| `UserStatus` | `ACTIVE`, `SUSPENDED`, `BLOCKED` | User không `ACTIVE` không đặt vé được |

---

## 3. Dữ liệu mẫu (seed) để test

### Users

| userId | Email | Role | Status |
|---|---|---|---|
| `10000000-0000-0000-0000-000000000001` | customer.demo@example.com | CUSTOMER | ACTIVE |
| `10000000-0000-0000-0000-000000000002` | staff.demo@example.com | STAFF | ACTIVE |
| `10000000-0000-0000-0000-000000000003` | blocked.demo@example.com | CUSTOMER | BLOCKED (đặt vé sẽ bị lỗi 400) |

### Movies

| movieId | Tên | Thời lượng | Status |
|---|---|---|---|
| `20000000-0000-0000-0000-000000000001` | Avatar: The Way of Water | 192 | NOW_SHOWING |
| `20000000-0000-0000-0000-000000000002` | Dune: Part Two | 166 | NOW_SHOWING |
| `20000000-0000-0000-0000-000000000003` | Inside Out 2 | 96 | COMING_SOON |

### Cinemas / Screens

| cinemaId | Rạp | screenId | Phòng |
|---|---|---|---|
| `30000000-0000-0000-0000-000000000001` | MV Cinema Landmark 81 | `32000000-0000-0000-0000-000000000001` | Screen 1 (2D, 8 ghế) |
| `30000000-0000-0000-0000-000000000002` | MV Cinema Vincom Dong Khoi | `32000000-0000-0000-0000-000000000002` | IMAX 1 (IMAX, 8 ghế) |

### Showtimes và ghế suất chiếu (`showtimeSeatId` dùng khi đặt vé)

**Suất 1** `50000000-0000-0000-0000-000000000001` – Avatar, Landmark 81, Screen 1

| Ghế | showtimeSeatId | Loại | Giá | Trạng thái seed |
|---|---|---|---|---|
| A1 | `51000000-0000-0000-0000-000000000001` | STANDARD | 100000 | HELD (đã bị giữ) |
| A2 | `51000000-0000-0000-0000-000000000002` | STANDARD | 100000 | HELD (đã bị giữ) |
| A3 | `51000000-0000-0000-0000-000000000003` | STANDARD | 100000 | AVAILABLE |
| A4 | `51000000-0000-0000-0000-000000000004` | STANDARD | 100000 | AVAILABLE |
| B1 | `51000000-0000-0000-0000-000000000005` | VIP | 150000 | AVAILABLE |
| B2 | `51000000-0000-0000-0000-000000000006` | VIP | 150000 | AVAILABLE |
| B3 | `51000000-0000-0000-0000-000000000007` | VIP | 150000 | AVAILABLE |
| B4 | `51000000-0000-0000-0000-000000000008` | VIP | 150000 | BLOCKED |

**Suất 2** `50000000-0000-0000-0000-000000000002` – Dune: Part Two, Vincom Dong Khoi, IMAX 1, bắt đầu `2030-01-16 20:00`

| Ghế | showtimeSeatId | Loại | Giá |
|---|---|---|---|
| A1 | `51000000-0000-0000-0000-000000000101` | STANDARD | 120000 |
| A2 | `51000000-0000-0000-0000-000000000102` | STANDARD | 120000 |
| A3 | `51000000-0000-0000-0000-000000000103` | STANDARD | 120000 |
| A4 | `51000000-0000-0000-0000-000000000104` | STANDARD | 120000 |
| B1 | `51000000-0000-0000-0000-000000000105` | VIP | 180000 |
| B2 | `51000000-0000-0000-0000-000000000106` | VIP | 180000 |
| B3 | `51000000-0000-0000-0000-000000000107` | VIP | 180000 |
| B4 | `51000000-0000-0000-0000-000000000108` | VIP | 180000 |

(Tất cả ghế suất 2 đều AVAILABLE trong seed.)

---

## 4. Chi tiết endpoint

### 4.1 Movie

#### 4.1.1 `GET /internal/movies/{id}` – Chi tiết phim

Response `200`:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "20000000-0000-0000-0000-000000000002",
    "title": "Dune: Part Two",
    "originalTitle": "Dune: Part Two",
    "description": "Paul Atreides joins the Fremen while seeking justice for House Atreides.",
    "durationMinutes": 166,
    "releaseDate": "2024-03-01",
    "ageRating": "T16",
    "language": "English",
    "country": "United States",
    "posterUrl": "https://example.com/posters/dune-part-two.jpg",
    "status": "NOW_SHOWING",
    "createdAt": "2026-10-01T00:00:00Z",
    "updatedAt": "2026-10-01T00:00:00Z"
  },
  "timestamp": "2026-10-19T03:00:00Z"
}
```

Lỗi: `404` nếu không có phim.

---

### 4.2 Cinema / Screen

#### 4.2.1 `GET /internal/cinemas/{id}` – Chi tiết rạp

Response `200` – `data`:

```json
{
  "id": "30000000-0000-0000-0000-000000000001",
  "name": "MV Cinema Landmark 81",
  "brand": "MV Cinema",
  "address": "720A Dien Bien Phu, Ward 22",
  "city": "Ho Chi Minh City",
  "district": "Binh Thanh",
  "phone": "028-3000-0001",
  "status": "ACTIVE",
  "createdAt": "2026-10-01T00:00:00Z"
}
```

#### 4.2.2 `GET /internal/screens/{id}` – Chi tiết phòng chiếu

Response `200` – `data`:

```json
{
  "id": "32000000-0000-0000-0000-000000000001",
  "cinemaId": "30000000-0000-0000-0000-000000000001",
  "screenTypeId": "31000000-0000-0000-0000-000000000001",
  "screenTypeName": "2D",
  "name": "Screen 1",
  "totalSeats": 8,
  "status": "ACTIVE"
}
```

#### 4.2.3 `GET /internal/screens/{id}/layout` – Sơ đồ ghế vật lý

Dùng để **vẽ sơ đồ phòng** (hàng/cột, loại ghế). Đây là ghế **vật lý** (`seatId`), **không** có trạng thái trống/đã đặt theo suất chiếu và **không** có `showtimeSeatId` (xem mục 6).

Response `200` – `data`:

```json
{
  "screen": {
    "id": "32000000-0000-0000-0000-000000000001",
    "cinemaId": "30000000-0000-0000-0000-000000000001",
    "screenTypeId": "31000000-0000-0000-0000-000000000001",
    "screenTypeName": "2D",
    "name": "Screen 1",
    "totalSeats": 8,
    "status": "ACTIVE"
  },
  "seats": [
    {
      "id": "40000000-0000-0000-0000-000000000001",
      "screenId": "32000000-0000-0000-0000-000000000001",
      "seatTypeId": "33000000-0000-0000-0000-000000000001",
      "seatTypeName": "STANDARD",
      "priceMultiplier": 1.00,
      "rowLabel": "A",
      "seatNumber": 1,
      "label": "A1",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 4.3 Showtime

#### 4.3.1 `POST /api/v1/showtimes` – Tạo suất chiếu (admin)

Request body:

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `movieId` | UUID | Có | |
| `cinemaId` | UUID | Có | |
| `screenId` | UUID | Có | Phòng phải thuộc rạp |
| `startTime` | Instant | Có | Phải ở tương lai |
| `basePrice` | number | Có | `>= 0` (VND) |

```json
{
  "movieId": "20000000-0000-0000-0000-000000000001",
  "cinemaId": "30000000-0000-0000-0000-000000000001",
  "screenId": "32000000-0000-0000-0000-000000000001",
  "startTime": "2030-02-01T12:00:00Z",
  "basePrice": 90000
}
```

Response `201`:

```json
{
  "success": true,
  "message": "Showtime created successfully",
  "data": {
    "id": "b6a1f0c2-3c1e-4c8e-9a51-2f0c7d9e1a11",
    "movieId": "20000000-0000-0000-0000-000000000001",
    "screenId": "32000000-0000-0000-0000-000000000001",
    "movieTitle": "Avatar: The Way of Water",
    "cinemaId": "30000000-0000-0000-0000-000000000001",
    "cinemaName": "MV Cinema Landmark 81",
    "screenName": "Screen 1",
    "startTime": "2030-02-01T12:00:00Z",
    "endTime": "2030-02-01T15:12:00Z",
    "basePrice": 90000,
    "status": "SCHEDULED",
    "createdAt": "2026-10-19T03:00:00Z"
  },
  "timestamp": "2026-10-19T03:00:00Z"
}
```

`endTime` do server tính theo thời lượng phim. Giá từng ghế = `basePrice` × `priceMultiplier` của loại ghế.

Lỗi `400` với `message`: `startTime must be in the future`, `basePrice must be greater than or equal to 0`, `Movie is not available for scheduling`, `Cinema is not active`, `Screen does not belong to requested cinema`, `Screen is not active`, `Screen layout has no seats`.

---

### 4.4 Booking

#### `BookingResponse` (dùng chung cho mọi API booking)

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | UUID | bookingId |
| `bookingCode` | string | Mã hiển thị cho khách, dạng `BK12345678123` |
| `userId` | UUID | |
| `showtimeId` | UUID | |
| `movieTitle`, `cinemaName`, `screenName` | string | Snapshot lúc đặt |
| `startTime` | Instant | Giờ chiếu |
| `subtotal` | number | Tổng giá ghế |
| `discountAmount` | number \| null | Hiện chưa dùng |
| `totalAmount` | number | Số tiền phải trả |
| `currency` | string | Luôn `"VND"` |
| `status` | BookingStatus | |
| `expiresAt` | Instant | **Hạn chót thanh toán**. FE hiển thị đồng hồ đếm ngược đến thời điểm này |
| `holdId` | UUID | Mã giữ ghế bên showtime-service |
| `paidPaymentId` | UUID \| null | Payment đã thanh toán thành công |
| `createdAt`, `updatedAt`, `confirmedAt`, `cancelledAt` | Instant \| null | |
| `version` | number | Dùng nội bộ |
| `seats` | `BookingSeatResponse[]` | |

`BookingSeatResponse`:

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | UUID | |
| `bookingId` | UUID | |
| `showtimeSeatId` | UUID | |
| `seatId` | UUID | Ghế vật lý |
| `seatLabel` | string | `"A3"` |
| `unitPrice` | number | Giá ghế do server quyết định |
| `ticketCode` | string | Mã vé `TKT-XXXXXXXXXXXX` (hiển thị/QR khi booking `CONFIRMED`) |
| `status` | BookingSeatStatus | |

#### 4.4.1 `POST /api/v1/bookings` – Đặt vé

Headers: `X-User-Id` (bắt buộc), `Idempotency-Key` (bắt buộc), `Content-Type: application/json`.

Request body – **FE chỉ cần gửi `showtimeId` và `seats[].showtimeSeatId`**. Tên phim, giá, hạn thanh toán… đều do server tự lấy từ showtime-service; các field khác trong DTO bị bỏ qua (không gửi `userId` trong body, server chỉ dùng header).

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `showtimeId` | UUID | Có | |
| `seats` | array | Có | Không rỗng |
| `seats[].showtimeSeatId` | UUID | Có | |

```json
{
  "showtimeId": "50000000-0000-0000-0000-000000000002",
  "seats": [
    { "showtimeSeatId": "51000000-0000-0000-0000-000000000105" },
    { "showtimeSeatId": "51000000-0000-0000-0000-000000000106" }
  ]
}
```

> Lưu ý idempotency: server so sánh **toàn bộ body**. Gửi lại cùng `Idempotency-Key` với body y hệt → trả lại booking cũ (`201`). Cùng key nhưng body khác → `409`. Mỗi lần người dùng chọn ghế mới và bấm đặt → sinh key mới.

Response `201`:

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "id": "8f14e45f-ceea-367a-9a36-dedd4bea2543",
    "bookingCode": "BK60291234517",
    "userId": "10000000-0000-0000-0000-000000000001",
    "showtimeId": "50000000-0000-0000-0000-000000000002",
    "movieTitle": "Dune: Part Two",
    "cinemaName": "MV Cinema Vincom Dong Khoi",
    "screenName": "IMAX 1",
    "startTime": "2030-01-16T20:00:00Z",
    "subtotal": 360000.00,
    "discountAmount": null,
    "totalAmount": 360000.00,
    "status": "PENDING",
    "expiresAt": "2026-10-19T03:07:00Z",
    "createdAt": "2026-10-19T03:00:00Z",
    "confirmedAt": null,
    "cancelledAt": null,
    "holdId": "0d7c2a52-6a7e-4d0f-9a36-5c1e2f3a4b5c",
    "currency": "VND",
    "paidPaymentId": null,
    "version": 0,
    "updatedAt": "2026-10-19T03:00:00Z",
    "seats": [
      {
        "id": "a1b2c3d4-0000-0000-0000-000000000001",
        "bookingId": "8f14e45f-ceea-367a-9a36-dedd4bea2543",
        "showtimeSeatId": "51000000-0000-0000-0000-000000000105",
        "seatId": "40000000-0000-0000-0000-000000000105",
        "seatLabel": "B1",
        "unitPrice": 180000.00,
        "ticketCode": "TKT-3F9A1C2B7D4E",
        "status": "ACTIVE"
      },
      {
        "id": "a1b2c3d4-0000-0000-0000-000000000002",
        "bookingId": "8f14e45f-ceea-367a-9a36-dedd4bea2543",
        "showtimeSeatId": "51000000-0000-0000-0000-000000000106",
        "seatId": "40000000-0000-0000-0000-000000000106",
        "seatLabel": "B2",
        "unitPrice": 180000.00,
        "ticketCode": "TKT-8B2E6D0A1F3C",
        "status": "ACTIVE"
      }
    ]
  },
  "timestamp": "2026-10-19T03:00:00Z"
}
```

Ghế được giữ **10 phút**; `expiresAt` = hết giữ ghế − 3 phút (≈ **7 phút** sau khi đặt). Link PayOS **không** có trong response này, xem luồng ở mục 5.

Lỗi:

| HTTP | `message` (ví dụ) | Khi nào |
|---|---|---|
| 400 | `Invalid request payload` (+ `errors`) | Thiếu `showtimeId` hoặc `seats` rỗng |
| 400 | `Idempotency-Key header is required when creating a booking` | Thiếu header |
| 400 | `User is not active` | User bị khoá (user demo 0003) |
| 400 | `Showtime or seats not found / not bookable` | Sai id suất/ghế, suất không mở bán |
| 401 | `Missing required header X-User-Id` | |
| 409 | `Selected seats are no longer available` | Ghế đã bị người khác giữ/đặt → FE báo và cho chọn lại |
| 409 | `Idempotency key reused with different request payload` | |
| 503 | | showtime-service/user-service đang lỗi |

#### 4.4.2 `GET /api/v1/bookings` – Danh sách booking của tôi

Header `X-User-Id`. Chỉ trả về booking của chính user này. Không phân trang.

Response `200` – `data`: `BookingResponse[]`.

#### 4.4.3 `GET /api/v1/bookings/{id}` – Chi tiết booking

Header `X-User-Id`. Response `200` – `data`: `BookingResponse`.

Lỗi: `404` nếu không tồn tại **hoặc** là booking của người khác.

#### 4.4.4 `GET /api/v1/bookings/code/{bookingCode}` – Tra theo mã

Header `X-User-Id`. Ví dụ `GET /api/v1/bookings/code/BK60291234517`. Response `200` – `data`: `BookingResponse`. Lỗi giống 4.4.3.

#### 4.4.5 `POST /api/v1/bookings/{id}/cancel` – Huỷ booking

Header `X-User-Id`. Body **không bắt buộc**:

```json
{ "reason": "Đổi ý" }
```

(không gửi body → lý do mặc định `"Cancelled by user"`).

Chỉ huỷ được khi `status = PENDING` (chưa thanh toán). Ghế được nhả ra.

Response `200`:

```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "data": { "...BookingResponse": "status = CANCELLED, cancelledAt có giá trị, seats[].status = CANCELLED" },
  "timestamp": "2026-10-19T03:01:00Z"
}
```

Lỗi: `400` (booking không ở `PENDING`, hoặc không phải của user), `404`.

#### 4.4.6 `POST /api/v1/bookings/{id}/payment-retry` – Thanh toán lại

Header `X-User-Id`, không có body. Dùng khi payment trước đó `FAILED` / `CANCELLED` / `EXPIRED` mà booking vẫn `PENDING`.

Điều kiện: booking `PENDING`, chưa quá `expiresAt`, tối đa **3 lần** tạo link thanh toán cho mỗi booking.

Response `202 Accepted`:

```json
{
  "success": true,
  "message": "New payment link requested",
  "data": { "...BookingResponse": "status vẫn PENDING" },
  "timestamp": "2026-10-19T03:03:00Z"
}
```

Link mới được tạo **bất đồng bộ**: sau khi nhận `202`, FE poll `GET /api/v1/payments/by-booking/{bookingId}` cho đến khi nhận được payment có **`id` khác** payment cũ (mục 5, bước 7).

Lỗi `400`: `Maximum payment attempts reached (3)`, `Booking has already expired`, booking không ở `PENDING`, `Access denied to pay this booking`. `404` nếu không có booking.

---

### 4.5 Payment (PayOS)

Payment **không được FE tạo trực tiếp**. Khi booking được tạo, booking-service gửi lệnh qua Kafka và payment-service tạo link PayOS sau vài giây. FE lấy link bằng cách poll API 4.5.1.

#### `PaymentResponse`

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | UUID | paymentId |
| `bookingId` | UUID | |
| `userId` | UUID | |
| `paymentCode` | string | `PAY-{orderCode}` |
| `provider` | string | `"PAYOS"` |
| `paymentMethod` | string | `"PAYOS"` |
| `status` | PaymentStatus | |
| `amount` | number | Bằng `totalAmount` của booking |
| `currency` | string | `"VND"` |
| `checkoutUrl` | string \| null | **Link trang thanh toán PayOS**, redirect người dùng tới đây |
| `qrCode` | string \| null | Chuỗi VietQR, FE có thể tự render thành mã QR (ví dụ thư viện `qrcode.react`) |
| `payosOrderCode` | number | Mã đơn bên PayOS |
| `gatewayTransactionId` | string \| null | Mã giao dịch PayOS sau khi trả tiền |
| `failureReason` | string \| null | Lý do thất bại |
| `paidAt` | Instant \| null | |
| `expiredAt` | Instant \| null | Link hết hạn lúc này |
| `createdAt`, `updatedAt` | Instant | |

Ví dụ:

```json
{
  "id": "c3e1b7a0-1111-4c1b-9d0e-7a2f3b4c5d6e",
  "bookingId": "8f14e45f-ceea-367a-9a36-dedd4bea2543",
  "userId": "10000000-0000-0000-0000-000000000001",
  "paymentCode": "PAY-1760842800123",
  "provider": "PAYOS",
  "paymentMethod": "PAYOS",
  "status": "PENDING",
  "amount": 360000.00,
  "currency": "VND",
  "checkoutUrl": "https://pay.payos.vn/web/abc123def456",
  "qrCode": "00020101021238570010A000000727012700069704220113VQRQ...6304ABCD",
  "payosOrderCode": 1760842800123,
  "gatewayTransactionId": null,
  "failureReason": null,
  "paidAt": null,
  "expiredAt": "2026-10-19T03:07:00Z",
  "createdAt": "2026-10-19T03:00:02Z",
  "updatedAt": "2026-10-19T03:00:02Z"
}
```

#### 4.5.1 `GET /api/v1/payments/by-booking/{bookingId}` – Payment mới nhất của booking

Header `X-User-Id`. Response `200` – `data`: `PaymentResponse` (lần thanh toán mới nhất).

**`404` ngay sau khi tạo booking là bình thường** (payment đang được tạo) → poll lại mỗi 1–2 giây, tối đa ~20 giây. `404` cũng trả về nếu booking không phải của user.

#### 4.5.2 `GET /api/v1/payments/{id}` – Chi tiết payment

Header `X-User-Id`. Response `200` – `data`: `PaymentResponse`. `404` nếu không tồn tại / không phải của user.

#### 4.5.3 `POST /api/v1/payments/by-booking/{bookingId}/sync` – Kiểm tra lại với PayOS

Header `X-User-Id`, không có body. Server **hỏi trực tiếp PayOS** trạng thái payment mới nhất của booking, cập nhật và trả về. Gọi khi người dùng quay lại từ trang PayOS (không tin query string trên URL redirect).

Response `200` – `data`: `PaymentResponse` (ví dụ `status: "SUCCESS"`, `paidAt` có giá trị).

#### 4.5.4 `POST /api/v1/payments/{id}/sync`

Giống 4.5.3 nhưng theo `paymentId`.

> `POST /api/v1/payments/payos/webhook` là endpoint **PayOS gọi**, FE **không** dùng.

---

### 4.6 Gateway – demo rate limit

Không phải nghiệp vụ, dùng cho trang demo "chống spam". Đều trả về:

```json
{
  "success": true,
  "message": "Gateway ping is reachable",
  "data": {
    "status": "pong",
    "caller": { "userId": "user-A", "apiKey": "part****", "clientIp": "127.0.0.1" }
  },
  "timestamp": "2026-10-19T03:00:00Z"
}
```

| Path | Header | Giới hạn (bucket / nạp lại) |
|---|---|---|
| `GET /rate-limit/user/ping` | `X-User-Id` (bất kỳ chuỗi) | 3 request, +1/giây, theo user |
| `GET /rate-limit/ip/ping` | – | 5 request, +1/giây, theo IP |
| `GET /rate-limit/api-key/ping` | `X-Api-Key` | 5 request, +2/giây, theo API key |

Response header luôn có `X-RateLimit-Remaining`, `X-RateLimit-Burst-Capacity`, `X-RateLimit-Replenish-Rate`, `X-RateLimit-Requested-Tokens`.

Vượt giới hạn → `429` (có header `Retry-After` tính bằng giây):

```json
{
  "success": false,
  "status": 429,
  "error": "Too Many Requests",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "path": "/rate-limit/user/ping",
  "timestamp": "2026-10-19T03:00:00Z"
}
```

Thiếu `X-User-Id` / `X-Api-Key` → `401`, `code: "RATE_LIMIT_IDENTITY_MISSING"`.

Các route có load balancer (`/api/v1/**`) trả thêm header `X-Served-By` = instance đã xử lý (dùng cho demo cân bằng tải).

---

### 4.7 Catalogue: phim, rạp, suất chiếu (API public mới)

Quy ước chung:
- GET không cần header. API ghi cần `X-User-Role: ADMIN` (thiếu → `403`).
- Danh sách có phân trang: `?page=0&size=20` (size tối đa 100). `data` có dạng:
  ```json
  { "items": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
  ```
- **Xoá là xoá mềm**: phim → `ARCHIVED`, rạp → `CLOSED`, suất → `CANCELLED`. Người thường không thấy phim `ARCHIVED` và rạp `CLOSED` (chi tiết trả `404`).

#### 4.7.1 Movie

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/movies?status=NOW_SHOWING&q=dune&page=0&size=20` | `status` và `q` (tìm theo tên) không bắt buộc |
| GET | `/api/v1/movies/{id}` | Trả `MovieResponse` như 4.1.1 |
| POST | `/api/v1/movies` | Body `MovieRequest` → `201` |
| PUT | `/api/v1/movies/{id}` | Body `MovieRequest`, **thay toàn bộ** (gửi đủ field) |
| PATCH | `/api/v1/movies/{id}/status` | `{ "status": "NOW_SHOWING" }` |
| DELETE | `/api/v1/movies/{id}` | Lưu trữ phim (`ARCHIVED`) |

```json
// MovieRequest
{ "title": "Dune: Part Three", "originalTitle": null, "description": "...", "durationMinutes": 165,
  "releaseDate": "2026-12-18", "ageRating": "T13", "language": "en", "country": "US",
  "posterUrl": "https://...", "status": "COMING_SOON" }
```
- Bắt buộc: `title`, `durationMinutes` (1–600). `status` chỉ dùng khi tạo: `COMING_SOON` (mặc định) hoặc `NOW_SHOWING`.
- Đổi trạng thái hợp lệ: `COMING_SOON → NOW_SHOWING ⇄ ENDED`, trạng thái nào cũng có thể sang `ARCHIVED`. Phim `ARCHIVED` không sửa được (`400`).
- Chỉ phim `NOW_SHOWING` mới tạo được suất chiếu.

#### 4.7.2 Cinema

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/cinemas?city=Ho Chi Minh&q=cgv&page=0&size=20` | `city`, `status`, `q` không bắt buộc |
| GET | `/api/v1/cinemas/{id}` | Trả `CinemaResponse` như 4.2.1 |
| GET | `/api/v1/cinemas/{id}/screens` | Mảng `ScreenResponse` như 4.2.2 |
| POST | `/api/v1/cinemas` | Body `CinemaRequest` → `201` |
| PUT | `/api/v1/cinemas/{id}` | Thay toàn bộ |
| PATCH | `/api/v1/cinemas/{id}/status` | `{ "status": "INACTIVE" }` |
| DELETE | `/api/v1/cinemas/{id}` | Đóng rạp (`CLOSED`, không mở lại được) |

```json
// CinemaRequest
{ "name": "CGV Landmark 81", "brand": "CGV", "address": "720A Dien Bien Phu", "city": "Ho Chi Minh",
  "district": "Binh Thanh", "phone": "1900 6017", "status": "ACTIVE" }
```
- Bắt buộc: `name`, `address`, `city`. `ACTIVE ⇄ INACTIVE`; `CLOSED` là trạng thái cuối. Chỉ rạp `ACTIVE` nhận suất chiếu mới.

#### 4.7.3 Showtime

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/showtimes?movieId=&cinemaId=&screenId=&date=2026-10-25&page=0&size=20` | Sắp xếp theo giờ chiếu. `date` = một ngày theo **giờ Việt Nam**; có thể dùng `from` / `to` (ISO) thay cho `date` |
| GET | `/api/v1/showtimes/{id}` | `ShowtimeResponse` như 4.3.1 |
| GET | `/api/v1/showtimes/{id}/seats` | **Sơ đồ ghế của suất** (xem dưới) |
| PUT | `/api/v1/showtimes/{id}` | `{ "startTime": "2026-10-25T15:00:00Z", "basePrice": 80000 }`, gửi 1 hoặc cả 2 field |
| DELETE | `/api/v1/showtimes/{id}` | Huỷ suất (`CANCELLED`, mọi ghế `BLOCKED`) |

```json
// GET /api/v1/showtimes/{id}/seats → data
[ { "showtimeSeatId": "51000000-0000-0000-0000-000000000003", "seatId": "...", "seatLabel": "A3",
    "seatType": "STANDARD", "price": 100000.00, "status": "AVAILABLE" } ]
```
- Dùng `showtimeSeatId` khi đặt vé (4.4.1). `status`: `AVAILABLE` / `HELD` / `BOOKED` / `BLOCKED`; chỉ cho chọn ghế `AVAILABLE`.
- Người thường chỉ thấy suất `SCHEDULED` / `SELLING` **chưa chiếu**. Admin gửi thêm `status=CANCELLED&status=ENDED` để xem các suất khác.
- Quy tắc admin:
  - Một phòng không được có 2 suất chồng giờ (cách nhau tối thiểu 15 phút dọn phòng) → `409`.
  - Suất đã có người giữ hoặc mua ghế thì **không sửa / huỷ được** → `409`.
  - Chỉ sửa được suất chưa bắt đầu.
  - Đổi giá thì giá các ghế đổi theo tỉ lệ (ghế VIP vẫn gấp 1,5 lần).

---

## 5. Luồng đặt vé và thanh toán (FE cần làm đúng thứ tự)

```
Chọn suất → chọn ghế → POST /bookings → poll payment → redirect PayOS
→ PayOS redirect về /payment/success|cancel → POST sync → poll booking → hiển thị vé
```

1. **Chọn suất chiếu và ghế.** Lấy `showtimeId` và các `showtimeSeatId` (hiện tại từ dữ liệu seed, xem mục 6).
2. **Đặt vé:** `POST /api/v1/bookings` với `X-User-Id` và `Idempotency-Key` mới.
   - `201` → lưu `booking.id`, hiển thị tổng tiền và đếm ngược đến `expiresAt`.
   - `409` → báo "Ghế vừa có người chọn", tải lại và cho chọn ghế khác.
3. **Lấy link thanh toán:** poll `GET /api/v1/payments/by-booking/{bookingId}` mỗi 1–2 giây cho đến khi `200` và có `checkoutUrl`.
4. **Trước khi chuyển trang**, lưu `bookingId` (ví dụ `localStorage.setItem('pendingBookingId', id)`), vì URL PayOS trả về không chứa bookingId.
5. **Thanh toán:** `window.location.href = payment.checkoutUrl` (hoặc hiển thị `qrCode` để quét ngay trên trang).
6. **PayOS redirect về FE:**
   - Thành công: `http://localhost:3000/payment/success?code=00&id=...&cancel=false&status=PAID&orderCode=...`
   - Huỷ: `http://localhost:3000/payment/cancel?code=00&id=...&cancel=true&status=CANCELLED&orderCode=...`

   Trên cả hai trang: đọc `pendingBookingId`, gọi `POST /api/v1/payments/by-booking/{bookingId}/sync`. **Không** tin `status` trên URL.
   - `status = SUCCESS` → poll `GET /api/v1/bookings/{bookingId}` mỗi 1–2 giây cho đến khi booking `CONFIRMED` (có thể qua `CONFIRMING`), rồi hiển thị vé: `bookingCode`, danh sách `seats[].seatLabel` + `ticketCode`.
   - `status = CANCELLED | FAILED | EXPIRED` → hiển thị "Thanh toán chưa thành công", nút **Thanh toán lại** và **Huỷ đặt vé**.
   - `status = PENDING | PROCESSING` → đợi vài giây rồi sync lại.
7. **Thanh toán lại:** `POST /api/v1/bookings/{id}/payment-retry` (`202`), nhớ `id` payment cũ, poll `GET /api/v1/payments/by-booking/{bookingId}` đến khi `data.id !== oldPaymentId`, rồi quay lại bước 4.
8. **Huỷ:** `POST /api/v1/bookings/{id}/cancel` (chỉ khi `PENDING`).
9. **Hết giờ:** khi đếm ngược về 0, booking sẽ thành `EXPIRED` và ghế được nhả. FE gọi lại `GET /api/v1/bookings/{id}` để cập nhật và không cho thanh toán nữa.

Trang **"Vé của tôi"**: `GET /api/v1/bookings`, nhóm theo `status`. Booking `PENDING` còn hạn hiển thị nút "Tiếp tục thanh toán" (dùng 4.5.1 lấy `checkoutUrl`).

---

## 6. Chưa có API (FE tạm dùng dữ liệu mock/seed)

| Chức năng | Tình trạng | Gợi ý cho FE |
|---|---|---|
| Đăng nhập / đăng ký / JWT | Chưa có | Màn hình chọn user demo, lưu `userId` vào `localStorage` và gửi `X-User-Id` |
| Danh sách phim, rạp, lịch chiếu, sơ đồ ghế theo suất | **Đã có** (mục 4.7) | Bỏ mock, gọi API 4.7 |
| CRUD phòng chiếu / sơ đồ ghế vật lý | Chưa có | Chỉ đọc được qua `/api/v1/cinemas/{id}/screens` |
| Thông tin user (profile) | Chỉ có API nội bộ, chưa qua Gateway | Mock theo bảng user demo |

FE nên tách lớp gọi API (ví dụ `src/api/*.ts`) để khi backend bổ sung các API trên chỉ cần thay phần mock.

---

## 7. TypeScript types

```ts
export type UUID = string;
export type ISODateTime = string; // Instant, UTC

export interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;
  timestamp: ISODateTime;
}

export interface ApiError {
  success: false;
  status: number;
  error: string;
  message: string;
  path: string;
  code?: string;                    // chỉ có ở lỗi rate limit của gateway
  errors?: Record<string, string>;  // lỗi validate theo field
  timestamp: ISODateTime;
}

export type BookingStatus =
  | 'PENDING' | 'CONFIRMING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'REFUND_PENDING' | 'REFUNDED';
export type BookingSeatStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'REFUNDED';
export type PaymentStatus =
  | 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED' | 'REFUND_PENDING' | 'REFUNDED';
export type ShowtimeStatus = 'SCHEDULED' | 'SELLING' | 'CANCELLED' | 'ENDED';
export type MovieStatus = 'COMING_SOON' | 'NOW_SHOWING' | 'ENDED' | 'ARCHIVED';
export type CinemaStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED';
export type ScreenStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE';
export type SeatStatus = 'ACTIVE' | 'BROKEN' | 'INACTIVE';

export interface Movie {
  id: UUID;
  title: string;
  originalTitle: string | null;
  description: string | null;
  durationMinutes: number;
  releaseDate: string | null; // YYYY-MM-DD
  ageRating: string | null;
  language: string | null;
  country: string | null;
  posterUrl: string | null;
  status: MovieStatus;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface Cinema {
  id: UUID;
  name: string;
  brand: string | null;
  address: string;
  city: string;
  district: string | null;
  phone: string | null;
  status: CinemaStatus;
  createdAt: ISODateTime;
}

export interface Screen {
  id: UUID;
  cinemaId: UUID;
  screenTypeId: UUID;
  screenTypeName: string;
  name: string;
  totalSeats: number;
  status: ScreenStatus;
}

export interface Seat {
  id: UUID;
  screenId: UUID;
  seatTypeId: UUID;
  seatTypeName: string;
  priceMultiplier: number;
  rowLabel: string;
  seatNumber: number;
  label: string;
  status: SeatStatus;
}

export interface ScreenLayout {
  screen: Screen;
  seats: Seat[];
}

export interface CreateShowtimeRequest {
  movieId: UUID;
  cinemaId: UUID;
  screenId: UUID;
  startTime: ISODateTime;
  basePrice: number;
}

export interface Showtime {
  id: UUID;
  movieId: UUID;
  screenId: UUID;
  movieTitle: string;
  cinemaId: UUID;
  cinemaName: string;
  screenName: string;
  startTime: ISODateTime;
  endTime: ISODateTime;
  basePrice: number;
  status: ShowtimeStatus;
  createdAt: ISODateTime;
}

export interface CreateBookingRequest {
  showtimeId: UUID;
  seats: { showtimeSeatId: UUID }[];
}

export interface CancelBookingRequest {
  reason?: string;
}

export interface BookingSeat {
  id: UUID;
  bookingId: UUID;
  showtimeSeatId: UUID;
  seatId: UUID;
  seatLabel: string;
  unitPrice: number;
  ticketCode: string;
  status: BookingSeatStatus;
}

export interface Booking {
  id: UUID;
  bookingCode: string;
  userId: UUID;
  showtimeId: UUID;
  movieTitle: string;
  cinemaName: string;
  screenName: string;
  startTime: ISODateTime;
  subtotal: number;
  discountAmount: number | null;
  totalAmount: number;
  status: BookingStatus;
  expiresAt: ISODateTime;
  createdAt: ISODateTime;
  confirmedAt: ISODateTime | null;
  cancelledAt: ISODateTime | null;
  holdId: UUID;
  currency: 'VND';
  paidPaymentId: UUID | null;
  version: number;
  updatedAt: ISODateTime;
  seats: BookingSeat[];
}

export interface Payment {
  id: UUID;
  bookingId: UUID;
  userId: UUID;
  paymentCode: string;
  provider: string;
  paymentMethod: string;
  status: PaymentStatus;
  amount: number;
  currency: 'VND';
  checkoutUrl: string | null;
  qrCode: string | null;
  payosOrderCode: number;
  gatewayTransactionId: string | null;
  failureReason: string | null;
  paidAt: ISODateTime | null;
  expiredAt: ISODateTime | null;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}
```
