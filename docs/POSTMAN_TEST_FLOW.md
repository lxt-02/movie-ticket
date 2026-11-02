# Test full luồng bằng Postman (trừ thanh toán)

Mọi request đi qua **Gateway `http://localhost:8080`**; token lấy từ **Keycloak `http://localhost:8180`**.

## 0. Chuẩn bị

```bash
docker compose --profile app up -d --build     # hoặc: docker compose up -d rồi chạy service từ IDE
```

Postman → tạo **Environment** với 2 biến (các biến khác script tự điền):

| Biến | Giá trị |
|---|---|
| `base` | `http://localhost:8080` |
| `kc`   | `http://localhost:8180/realms/mss301/protocol/openid-connect/token` |

Import curl: Postman → **Import → Raw text** → dán curl. Đoạn **Tests** (nếu có) dán vào tab *Scripts → Post-response* của request đó.

Tài khoản demo (mật khẩu đều `123456`): `admin` (ADMIN), `staff` (STAFF), `customer` (CUSTOMER), `blocked` (CUSTOMER bị khoá).

> Về thanh toán: tạo booking sẽ tự gửi `CREATE_PAYMENT` qua Kafka. Không test payment thì booking cứ ở **PENDING**;
> khoảng 7 phút (hạn giữ ghế 10 phút − 3 phút an toàn) + 90 giây sau sẽ tự **EXPIRED** và nhả ghế.

---

## 1. Đăng nhập (lấy token)

### 1.1 Token admin
```bash
curl --location '{{kc}}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=mss301-frontend' \
--data-urlencode 'username=admin' \
--data-urlencode 'password=123456'
```
Tests:
```js
pm.environment.set("adminToken", pm.response.json().access_token);
```

### 1.2 Token customer
```bash
curl --location '{{kc}}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=mss301-frontend' \
--data-urlencode 'username=customer' \
--data-urlencode 'password=123456'
```
Tests:
```js
pm.environment.set("customerToken", pm.response.json().access_token);
```

### 1.3 Token staff (dùng ở bước tranh ghế) và blocked
Giống 1.2, đổi `username=staff` → lưu `staffToken`; `username=blocked` → lưu `blockedToken`.

Token sống 30 phút; hết hạn (401) thì chạy lại bước 1.

---

## 2. Hồ sơ người dùng (tạo user trong DB từ token)

### 2.1 Customer: xem / tạo hồ sơ (gọi ngay sau khi đăng nhập)
```bash
curl --location '{{base}}/api/v1/users/me' \
--header 'Authorization: Bearer {{customerToken}}'
```
Tests:
```js
pm.environment.set("customerId", pm.response.json().data.id);
```

### 2.2 Cập nhật hồ sơ
```bash
curl --location --request PUT '{{base}}/api/v1/users/me' \
--header 'Authorization: Bearer {{customerToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "fullName": "Nguyễn Văn Khách",
  "avatarUrl": "https://example.com/avatar/khach.png"
}'
```

### 2.3 Admin: danh sách user
```bash
curl --location '{{base}}/api/v1/users?q=demo&page=0&size=20' \
--header 'Authorization: Bearer {{adminToken}}'
```

---

## 3. Duyệt & tìm kiếm (KHÔNG cần token)

### 3.1 Danh sách phim đang chiếu
```bash
curl --location '{{base}}/api/v1/movies?status=NOW_SHOWING&page=0&size=20'
```

### 3.2 Chi tiết phim (Avatar, dữ liệu mẫu)
```bash
curl --location '{{base}}/api/v1/movies/20000000-0000-0000-0000-000000000001'
```

### 3.3 Thể loại, người (đạo diễn / diễn viên)
```bash
curl --location '{{base}}/api/v1/movies/genres'
```
```bash
curl --location '{{base}}/api/v1/movies/persons?q=Tr%E1%BA%A5n&page=0&size=20'
```

### 3.4 Tìm kiếm phim (Elasticsearch): theo từ khoá, người, thể loại, độ tuổi
`sort`: relevance (mặc định) | newest | oldest | title | duration_asc | duration_desc.
```bash
curl --location '{{base}}/api/v1/search/movies?q=lat%20mat&page=0&size=12'
```
```bash
curl --location '{{base}}/api/v1/search/movies?person=Zendaya&genres=Action&ageRating=T16&sort=newest&page=0&size=12'
```
> Nếu kết quả rỗng ngay lúc mới chạy: chờ movie-service gửi lại dữ liệu sang search (vài giây), hoặc chạy bước 4.4.

### 3.5 Rạp, phòng, loại phòng / loại ghế
```bash
curl --location '{{base}}/api/v1/cinemas?city=Ho%20Chi%20Minh%20City'
```
```bash
curl --location '{{base}}/api/v1/cinemas/30000000-0000-0000-0000-000000000001/screens'
```
```bash
curl --location '{{base}}/api/v1/screens/32000000-0000-0000-0000-000000000001/layout'
```
```bash
curl --location '{{base}}/api/v1/screen-types'
```
```bash
curl --location '{{base}}/api/v1/seat-types'
```

### 3.6 Suất chiếu của một phim + sơ đồ ghế
```bash
curl --location '{{base}}/api/v1/showtimes?movieId=20000000-0000-0000-0000-000000000001&status=SELLING'
```
```bash
curl --location '{{base}}/api/v1/showtimes/50000000-0000-0000-0000-000000000001/seats'
```
> Suất mẫu `50000000-...-0001` (Avatar, đang SELLING): A1, A2 đang bị giữ bởi booking mẫu; A3, A4, B1–B3 trống; B4 hỏng.
> showtimeSeatId của ghế trống: A3 = `51000000-0000-0000-0000-000000000003`, A4 = `...0004`, B1 = `...0005`, B2 = `...0006`, B3 = `...0007`.

---

## 4. Admin tạo dữ liệu mới (phim → rạp → phòng → suất chiếu)

Bỏ qua mục này nếu chỉ muốn test đặt vé bằng dữ liệu mẫu (sang mục 5).

### 4.1 Tạo thể loại
```bash
curl --location '{{base}}/api/v1/movies/genres' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{ "name": "Thriller" }'
```
Tests:
```js
pm.environment.set("genreId", pm.response.json().data.id);
```

### 4.2 Tạo đạo diễn
```bash
curl --location '{{base}}/api/v1/movies/persons' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "name": "Victor Vũ",
  "biography": "Đạo diễn phim Việt Nam."
}'
```
Tests:
```js
pm.environment.set("personId", pm.response.json().data.id);
```

### 4.3 Tạo phim (phải NOW_SHOWING mới xếp suất chiếu được)
```bash
curl --location '{{base}}/api/v1/movies' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "title": "Thám Tử Kiên",
  "originalTitle": "Thám Tử Kiên: Kỳ Án Không Đầu",
  "description": "Phim trinh thám kinh dị lấy bối cảnh làng quê Việt Nam.",
  "durationMinutes": 131,
  "releaseDate": "2026-10-20",
  "ageRating": "T16",
  "language": "Vietnamese",
  "country": "Vietnam",
  "posterUrl": "https://example.com/posters/tham-tu-kien.jpg",
  "status": "NOW_SHOWING",
  "genreIds": ["{{genreId}}", "21000000-0000-0000-0000-000000000005"],
  "crew": [
    { "personId": "{{personId}}", "role": "DIRECTOR" }
  ]
}'
```
Tests:
```js
pm.environment.set("movieId", pm.response.json().data.id);
```
Sau vài giây phim xuất hiện trong search: `{{base}}/api/v1/search/movies?q=tham tu kien`.

### 4.4 (tuỳ chọn) Đồng bộ lại toàn bộ phim sang Elasticsearch
```bash
curl --location --request POST '{{base}}/api/v1/movies/search-sync' \
--header 'Authorization: Bearer {{adminToken}}'
```

### 4.5 Tạo rạp
```bash
curl --location '{{base}}/api/v1/cinemas' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "name": "MV Cinema Thủ Đức",
  "brand": "MV Cinema",
  "address": "216 Võ Văn Ngân, Bình Thọ",
  "city": "Ho Chi Minh City",
  "district": "Thu Duc",
  "phone": "028-3000-0003",
  "status": "ACTIVE"
}'
```
Tests:
```js
pm.environment.set("cinemaId", pm.response.json().data.id);
```

### 4.6 Tạo phòng chiếu kèm sơ đồ ghế (A, B thường; C VIP)
Loại phòng 2D = `31000000-0000-0000-0000-000000000001`; ghế STANDARD = `33000000-...-0001`, VIP = `33000000-...-0002`.
```bash
curl --location '{{base}}/api/v1/cinemas/{{cinemaId}}/screens' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "name": "Phòng 1",
  "screenTypeId": "31000000-0000-0000-0000-000000000001",
  "rows": [
    { "rowLabel": "A", "seatCount": 8, "seatTypeId": "33000000-0000-0000-0000-000000000001" },
    { "rowLabel": "B", "seatCount": 8, "seatTypeId": "33000000-0000-0000-0000-000000000001" },
    { "rowLabel": "C", "seatCount": 6, "seatTypeId": "33000000-0000-0000-0000-000000000002" }
  ]
}'
```
Tests:
```js
pm.environment.set("screenId", pm.response.json().data.screen.id);
```

### 4.7 Tạo suất chiếu (ngày mai, 19:00 giờ VN)
Pre-request script (tự tính ngày mai 12:00 UTC = 19:00 VN):
```js
const d = new Date(Date.now() + 24*3600*1000); d.setUTCHours(12,0,0,0);
pm.environment.set("showtimeStart", d.toISOString());
```
```bash
curl --location '{{base}}/api/v1/showtimes' \
--header 'Authorization: Bearer {{adminToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{
  "movieId": "{{movieId}}",
  "cinemaId": "{{cinemaId}}",
  "screenId": "{{screenId}}",
  "startTime": "{{showtimeStart}}",
  "basePrice": 90000
}'
```
Tests:
```js
pm.environment.set("showtimeId", pm.response.json().data.id);
```
> Suất mới ở trạng thái **SCHEDULED**; job chạy mỗi 60 giây chuyển sang **SELLING** khi còn ≤ 7 ngày tới giờ chiếu.
> Chờ ~1 phút rồi kiểm tra `GET {{base}}/api/v1/showtimes/{{showtimeId}}` thấy `"status": "SELLING"` mới đặt vé được.
> Tạo trùng giờ trên cùng phòng → 409 (lịch chồng nhau, có 15 phút dọn phòng).

### 4.8 Sơ đồ ghế của suất mới: lấy 2 ghế để đặt
```bash
curl --location '{{base}}/api/v1/showtimes/{{showtimeId}}/seats'
```
Tests (lấy 2 ghế trống đầu tiên):
```js
const free = pm.response.json().data.filter(s => s.status === "AVAILABLE");
pm.environment.set("seat1", free[0].showtimeSeatId);
pm.environment.set("seat2", free[1].showtimeSeatId);
```
Ghế VIP giá = 90 000 × 1.5 = 135 000.

---

## 5. Đặt vé (customer)

Nếu bỏ qua mục 4, đặt biến cho suất mẫu:
`showtimeId = 50000000-0000-0000-0000-000000000001`, `seat1 = 51000000-0000-0000-0000-000000000003` (A3),
`seat2 = 51000000-0000-0000-0000-000000000004` (A4).

### 5.1 Tạo booking
Pre-request script (mỗi lần bấm đặt vé = 1 key mới):
```js
pm.environment.set("idemKey", pm.variables.replaceIn("{{$guid}}"));
```
```bash
curl --location '{{base}}/api/v1/bookings' \
--header 'Authorization: Bearer {{customerToken}}' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: {{idemKey}}' \
--data-raw '{
  "showtimeId": "{{showtimeId}}",
  "seats": [
    { "showtimeSeatId": "{{seat1}}" },
    { "showtimeSeatId": "{{seat2}}" }
  ]
}'
```
Tests:
```js
const b = pm.response.json().data;
pm.environment.set("bookingId", b.id);
pm.environment.set("bookingCode", b.bookingCode);
pm.test("PENDING", () => pm.expect(b.status).to.eql("PENDING"));
```
Kết quả: 201, `status = PENDING`, `totalAmount` = tổng giá 2 ghế, `expiresAt` ≈ 7 phút sau.
Kiểm tra lại 3.6 / 4.8: 2 ghế đó chuyển **HELD**.

### 5.2 Gửi lại đúng request (cùng Idempotency-Key)
Tắt pre-request script của 5.1 (hoặc nhân bản request không có script) rồi gửi lại y hệt
→ 201, trả **cùng `bookingId`**, không giữ thêm ghế.

### 5.3 Xem booking
```bash
curl --location '{{base}}/api/v1/bookings/{{bookingId}}' \
--header 'Authorization: Bearer {{customerToken}}'
```
```bash
curl --location '{{base}}/api/v1/bookings/code/{{bookingCode}}' \
--header 'Authorization: Bearer {{customerToken}}'
```
```bash
curl --location '{{base}}/api/v1/bookings' \
--header 'Authorization: Bearer {{customerToken}}'
```

### 5.4 Huỷ booking → ghế được nhả
```bash
curl --location '{{base}}/api/v1/bookings/{{bookingId}}/cancel' \
--header 'Authorization: Bearer {{customerToken}}' \
--header 'Content-Type: application/json' \
--data-raw '{ "reason": "Đổi lịch" }'
```
→ `status = CANCELLED`; gọi lại sơ đồ ghế: 2 ghế về **AVAILABLE**.

---

## 6. Các trường hợp lỗi nên test

| # | Cách làm | Kết quả mong đợi |
|---|---|---|
| 6.1 | 5.1 nhưng **không** có header `Authorization` | 401 |
| 6.2 | 4.3 bằng `{{customerToken}}` | 403 (chỉ ADMIN) |
| 6.3 | 5.1 nhưng **thiếu** header `Idempotency-Key` | 400 "Thiếu header Idempotency-Key" |
| 6.4 | Giữ nguyên `idemKey` của 5.1 nhưng đổi `seat2` sang ghế khác | 409 "Idempotency-Key đã được dùng cho một yêu cầu khác" |
| 6.5 | Customer đặt seat1 (5.1, chưa huỷ), sau đó **staff** đặt cùng seat1 bằng `{{staffToken}}` + key mới | 409 "Ghế … vừa có người khác chọn" |
| 6.6 | 5.1 bằng `{{blockedToken}}` | 400 "Tài khoản không hoạt động hoặc đã bị khoá" |
| 6.7 | 5.3 xem booking của customer bằng `{{staffToken}}` | 404 (không lộ booking của người khác) |
| 6.8 | 5.4 huỷ lần 2 | 400 (booking không còn PENDING) |
| 6.9 | Đặt ghế B4 của suất mẫu (`51000000-0000-0000-0000-000000000008`, ghế hỏng) | 409 "Ghế B4 vừa có người khác chọn" |
| 6.10 | Gửi 5.1 liên tục > 3 lần/giây | 429 (rate limit đặt vé theo user) |
| 6.11 | 5.1 với `showtimeId` của suất còn SCHEDULED (vừa tạo ở 4.7, chưa tới 1 phút) | 409 "Suất chiếu hiện không mở đặt ghế" |

## 7. Hết hạn tự động (không thanh toán)

Tạo booking (5.1), không huỷ, chờ khoảng **9 phút** rồi gọi 5.3 → `status = EXPIRED`, ghế trở lại **AVAILABLE**.

## 8. Admin: theo dõi Kafka / DLT (tuỳ chọn)

```bash
curl --location '{{base}}/api/v1/bookings/admin/payment-events-dlt' \
--header 'Authorization: Bearer {{adminToken}}'
```
Xem message trên Kafka UI: http://localhost:8090 (topic `payment-commands`: thấy `CREATE_PAYMENT` / `CANCEL_PAYMENT` của các booking vừa tạo / huỷ).
Trace một request xuyên service: Zipkin http://localhost:9411.
