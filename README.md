# MSS301 – Hệ thống đặt vé xem phim (microservices)

Đồ án môn MSS301. Hệ thống gồm 7 service Spring Boot, một API Gateway, Eureka, và hạ tầng chạy bằng Docker (PostgreSQL, Kafka, Redis, Keycloak, Elasticsearch...). Mỗi service có database riêng.

## 1. Kiến trúc

| Thành phần | Cổng (trong container) | Vai trò |
|---|---|---|
| `api-gateway` | 8080 (**mở ra ngoài**) | Cổng vào duy nhất: kiểm tra token Keycloak, rate limit, load balancing |
| `eureka-service` | 8761 (mở ra ngoài) | Service discovery |
| `user-service` | 8081 | Hồ sơ người dùng, quản lý user (admin) |
| `showtime-service` | 8082 | Suất chiếu, sơ đồ ghế, giữ ghế |
| `booking-service` | 8083 | Đặt vé, vòng đời đơn, nhận event thanh toán |
| `cinema-service` | 8084 | Rạp, phòng chiếu, loại ghế |
| `movie-service` | 8085 | Phim, thể loại, đạo diễn/diễn viên |
| `payment-service` | 8086 | Thanh toán PayOS, webhook |
| `search-service` | 8087 | Tìm phim bằng Elasticsearch |

Các service không mở cổng ra máy host. Bên ngoài chỉ gọi được qua Gateway (`/internal/**` bị chặn).

### Luồng Kafka

| Topic | Chiều | Nội dung |
|---|---|---|
| `payment-commands` | booking → payment | `CREATE_PAYMENT`, `CANCEL_PAYMENT` |
| `payment-events` | payment → booking | link tạo xong, thanh toán thành công/thất bại/hết hạn/huỷ |
| `movie-events` | movie → search | đồng bộ phim sang Elasticsearch |
| `*.DLT` | consumer lỗi | message không xử lý được sau khi retry |

Gửi message dùng **Transactional Outbox** (bảng `outbox_messages` trong DB của mỗi service). Hợp đồng message booking ↔ payment nằm ở `contracts/booking-payment/`.

## 2. Yêu cầu

- Docker Desktop (Docker Compose v2)
- Postman (để test)
- Nếu chạy service từ IDE: JDK 21 và Maven (hoặc `mvnw` có sẵn trong từng module)
- RAM trống khoảng 6–8 GB khi chạy toàn bộ stack

## 3. Chạy hệ thống

### Cách A – Chạy toàn bộ bằng Docker (đơn giản nhất)

```bash
docker compose --profile app up -d --build
```

Lần build đầu mất vài phút. Chờ vài chục giây cho Eureka và Keycloak sẵn sàng, rồi kiểm tra: http://localhost:8761 phải thấy các service đã đăng ký.

Dừng:

```bash
docker compose --profile app down        # giữ dữ liệu
docker compose --profile app down -v     # xoá cả dữ liệu PostgreSQL/Grafana
```

### Cách B – Chỉ chạy hạ tầng, chạy service từ IDE

```bash
docker compose up -d
```

Với mỗi service: copy `.env.example` thành `.env` (ví dụ `services/booking-service/.env`), chỉnh nếu cần, rồi chạy class `*Application` trong IntelliJ. Thứ tự gợi ý: Eureka → các service → Gateway.

### Cấu hình thanh toán (PayOS)

Tạo `services/payment-service/.env` từ `.env.example` và điền `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`. **Không commit file `.env`.** Nếu không có khoá PayOS, bạn vẫn test được mọi luồng trừ thanh toán thật: đơn sẽ ở `PENDING` rồi tự `EXPIRED`.

### Các địa chỉ hữu ích

| Công cụ | URL |
|---|---|
| API Gateway (mọi API) | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Keycloak (lấy token) | http://localhost:8180 |
| Kafka UI (xem topic, message, DLT) | http://localhost:8090 |
| Zipkin (trace) | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| PostgreSQL | `localhost:5433` (user `postgres`, mật khẩu mặc định `12345`) |
| Kibana (tuỳ chọn) | `docker compose --profile kibana up -d kibana` → http://localhost:5601 |

## 4. Tài khoản demo

Mật khẩu đều là `123456`.

| Username | Vai trò |
|---|---|
| `admin` | ADMIN |
| `staff` | STAFF |
| `customer` | CUSTOMER |
| `blocked` | CUSTOMER bị khoá (để test từ chối đặt vé) |

Lấy token (client `mss301-frontend`, token sống 30 phút):

```bash
curl -X POST http://localhost:8180/realms/mss301/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=mss301-frontend" \
  -d "username=customer" -d "password=123456"
```

Gọi API với header `Authorization: Bearer <access_token>`.

## 5. Test bằng Postman

Thư mục `docs/` có sẵn:

| File | Dùng cho |
|---|---|
| `MSS301-Demo-Luong-Chinh.postman_collection.json` | Luồng chính từ đăng nhập đến đặt vé, thanh toán, quan sát Kafka |
| `MSS301-Booking-Payment-Kafka.postman_collection.json` | Demo riêng cho Kafka: outbox, retry, DLT, replay |
| `MSS301-Kafka-Search.postman_collection.json` | Đồng bộ phim sang Elasticsearch và tìm kiếm |
| `MSS301.postman_collection.json` | Bộ API tổng hợp |
| `POSTMAN_TEST_FLOW.md` | Hướng dẫn từng bước kèm script `Tests` |

Cách dùng:

1. Postman → **Import** file collection.
2. Tạo **Environment** với hai biến: `base = http://localhost:8080` và `kc = http://localhost:8180/realms/mss301/protocol/openid-connect/token`. Các biến khác (token, id) do script tự điền. Collection Kafka còn cần các biến `kui` (Kafka UI), `prom`, `zipkin` nếu bạn dùng các request quan sát.
3. Chạy theo thứ tự: đăng nhập → duyệt phim/suất → đặt vé → xem trạng thái.

### Luồng đặt vé tóm tắt

1. `GET /api/v1/showtimes/{id}/seats` – xem ghế trống.
2. `POST /api/v1/bookings` với header `Idempotency-Key` (mỗi lần đặt một key mới) – đơn `PENDING`, ghế được giữ khoảng 10 phút.
3. Booking gửi `CREATE_PAYMENT` qua Kafka; lấy link thanh toán bằng `GET /api/v1/payments/by-booking/{bookingId}` (link có thể xuất hiện sau vài giây, hãy gọi lại).
4. Thanh toán xong → payment gửi event → đơn chuyển `CONFIRMING` rồi `CONFIRMED`.
5. Thanh toán lỗi muốn thử lại: `POST /api/v1/bookings/{id}/payment-retry` (cùng đơn, không tạo đơn mới).
6. Huỷ đơn: `POST /api/v1/bookings/{id}/cancel`. Đơn không thanh toán sẽ tự `EXPIRED` và nhả ghế.

Trạng thái đơn: `PENDING` → `CONFIRMING` → `CONFIRMED`; nhánh `CANCELLED`, `EXPIRED`.

> Không có khoá PayOS thì bạn có thể giả lập webhook bằng request `POST /api/v1/payments/payos/webhook` trong collection.

## 6. Demo Kafka: retry, outbox, DLT

Các endpoint `admin` yêu cầu token `admin` hoặc `staff`.

| Mục đích | Request |
|---|---|
| Xem trạng thái outbox của booking | `GET /api/v1/bookings/admin/outbox` |
| Đưa message `FAILED` về `PENDING` sau khi sửa lỗi | `POST /api/v1/bookings/admin/outbox/requeue-failed` |
| Xem outbox của payment | `GET /api/v1/payments/admin/outbox` |
| Bật/tắt lỗi giả lập ở consumer (chỉ để demo) | `POST /api/v1/bookings/admin/chaos/payment-events?fail=true` |
| Xem message trong DLT | `GET /api/v1/bookings/admin/payment-events-dlt` |
| Xử lý lại các message trong DLT | `POST /api/v1/bookings/admin/payment-events-dlt/replay` (dừng: `/stop`) |

Kịch bản demo gợi ý:

1. **Broker sập:** `docker stop kafka` → đặt vé → đơn vẫn tạo, `GET .../admin/outbox` thấy message `PENDING` tăng `retry_count` → `docker start kafka` → message tự được gửi.
2. **Consumer lỗi → DLT:** bật chaos `fail=true`, thanh toán một đơn → sau khoảng 60 giây event vào `payment-events.DLT` (xem trong Kafka UI) → tắt chaos (`fail=false`) → gọi `replay` → đơn chuyển `CONFIRMED`.
3. **Chạy lại không sinh trùng:** gọi `replay` lần nữa, trạng thái đơn không đổi.

Chaos chỉ hoạt động khi `BOOKING_DEMO_CHAOS_ENABLED=true` (đã bật sẵn trong docker compose; **không bật ở môi trường thật**).

## 7. Chạy test

Test dùng Testcontainers nên cần Docker đang chạy.

```bash
cd services/booking-service && ./mvnw test      # Windows: mvnw.cmd test
cd services/payment-service && ./mvnw test
cd services/movie-service   && ./mvnw test
```

Test hợp đồng (`BookingPaymentContractTest`) ở booking và payment cùng đọc các file mẫu trong `contracts/booking-payment/`. Muốn đổi định dạng message: sửa file mẫu trước, rồi sửa code hai bên cho đến khi cả hai test xanh.

## 8. Xử lý sự cố thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Gọi API qua Gateway trả `401` | Token hết hạn (30 phút) hoặc thiếu header `Authorization`: lấy token mới |
| Danh sách phim tìm kiếm rỗng lúc mới chạy | Chờ vài giây cho movie-service gửi lại dữ liệu sang search, hoặc gọi `POST /api/v1/movies/search-sync` bằng token admin |
| Đơn mãi ở `PENDING` | Chưa thanh toán, hoặc Kafka/payment-service chưa sẵn sàng; xem `admin/outbox` và Kafka UI. Đơn tự hết hạn sau khoảng 7–10 phút |
| Link thanh toán xuất hiện chậm | Relay của payment quét mỗi 10 giây; gọi lại sau vài giây |
| Gọi API đặt vé nhiều quá bị `429` | Rate limit của Gateway; chờ vài giây rồi thử lại |
| `503` kèm `Retry-After` | Một service phụ thuộc tạm thời không sẵn sàng (circuit breaker); thử lại sau |
| Container không lên / hết RAM | Giảm số service chạy, hoặc dùng Cách B (chạy service từ IDE) |
| Muốn xoá sạch để chạy lại | `docker compose --profile app down -v` |

## 9. Cấu trúc thư mục

```
.
├── infras/            api-gateway, eureka-service, keycloak (realm), observability, postgres init
├── services/          user, showtime, booking, cinema, movie, payment, search
├── contracts/         hợp đồng message booking ↔ payment (dùng chung cho test hai phía)
├── docs/              Postman collections và hướng dẫn test
├── rule/              quy ước dự án (commit, schema, rate limit, payment...)
├── docker-compose.yml
└── Dockerfile         một image dùng chung cho mọi app Spring Boot (chọn app bằng biến APP)
```

Mỗi service theo kiến trúc phân lớp: `api` (REST, DTO) → `application` (use case, port) → `domain` (aggregate, quy tắc) → `infrastructure` (JPA, Kafka, Feign, bảo mật).

## 10. Quy ước commit

Dùng Conventional Commits, chi tiết ở `rule/commit/rule_commit.md`:

```
<type>(<scope>): <description>
```

Ví dụ: `feat(booking-service): add outbox_messages table with topic and key per message`.
