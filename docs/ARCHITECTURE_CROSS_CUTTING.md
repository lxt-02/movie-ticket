# MSS301 – Các phần dùng chung giữa service: xác thực, lan truyền header, Feign client, xử lý lỗi, Resilience4j

> Tài liệu tổng hợp các phần "cross-cutting" (đi xuyên qua nhiều service) của hệ thống đặt vé xem phim.
> API chi tiết cho Frontend xem `docs/FRONTEND_API.md`.

## Mục lục

1. [Bức tranh chung](#1-bức-tranh-chung)
2. [Build và chạy](#2-build-và-chạy)
3. [Xác thực bằng Keycloak (Gateway + từng service)](#3-xác-thực-bằng-keycloak)
4. [Interceptor và Header Propagation trong OpenFeign](#4-interceptor-và-header-propagation-trong-openfeign)
5. [Feign client trong infrastructure/client của từng service](#5-feign-client-trong-infrastructureclient-của-từng-service)
6. [Xử lý lỗi: tiếng Việt và lỗi Feign](#6-xử-lý-lỗi)
7. [Resilience4j trong luồng booking](#7-resilience4j-trong-luồng-booking)
8. [Phạm vi nghiệp vụ đã chốt](#8-phạm-vi-nghiệp-vụ-đã-chốt)
9. [Demo nhanh](#9-demo-nhanh)

---

## 1. Bức tranh chung

```
                         ┌──────────────┐
            login        │   Keycloak   │  realm mss301 (CUSTOMER / STAFF / ADMIN)
   FE ─────────────────▶ │   :8180      │
   │  access token (JWT) └──────────────┘
   │
   │ Authorization: Bearer <token>
   ▼
┌─────────────┐  kiểm tra token, tạo X-Correlation-Id,
│ API Gateway │  bỏ X-User-* giả, gắn X-User-Id / X-User-Role
│   :8080     │
└──────┬──────┘
       │ X-User-Id, X-User-Role, X-User-Email, X-User-Name, X-Correlation-Id
       ▼
┌──────────────┐  X-User-Id / X-User-Role → SecurityContextHolder, MDC [cid=...]
│ booking :8083│──Feign (interceptor: X-User-* + cid; Retry→CircuitBreaker→Bulkhead)──▶ showtime :8082 ──Feign──▶ cinema / movie
│              │──Feign ─────────────────────────────────────────────────────────▶ user :8081
└──────┬───────┘
       │ Kafka (outbox / inbox)
       ▼
   payment :8086 ── PayOS
```

| Vấn đề | Giải pháp | Mục |
|---|---|---|
| Ai đang gọi? Có quyền không? | Keycloak + JWT kiểm tra **một lần ở Gateway**; Gateway gửi danh tính xuống bằng header; service đưa vào `SecurityContextHolder` | 3 |
| Service gọi nhau thì danh tính và mã truy vết đi theo thế nào? | Feign `RequestInterceptor` chuyển tiếp `X-User-*`, `X-Correlation-Id` | 4 |
| Feign client và controller lệch nhau? | Interface dùng chung: controller `implements`, Feign `extends` | 5 |
| Thông báo lỗi cho người dùng | Tiếng Việt ở mọi nơi; lỗi của service khác được chuyển tiếp đúng | 6 |
| Service khác sập / chậm | Retry, Circuit Breaker, Bulkhead (Resilience4j) + timeout | 7 |

---

## 2. Build và chạy

### Cấu trúc module

```
pom.xml                        aggregator: build mọi module theo đúng thứ tự
infras/eureka-service          service registry
infras/api-gateway             Spring Cloud Gateway (WebFlux)
infras/keycloak/mss301-realm.json   realm được import khi Keycloak khởi động
services/{user,movie,cinema,showtime,payment,booking}-service
docker-compose.yml             redis, kafka, kafka-ui, keycloak
```

### Lệnh

```bash
docker compose up -d                 # redis, kafka, keycloak (đợi ~30s cho Keycloak)
mvn install -DskipTests              # ở thư mục gốc: build mọi module
# hoặc chạy từng service từ IDE (các service không phụ thuộc jar chung nào)
```

- Keycloak admin console: `http://localhost:8180` (`admin` / `admin`), realm `mss301`.
- Realm đã tồn tại thì Keycloak **không import lại**. Sửa `mss301-realm.json` xong phải
  `docker compose rm -sf keycloak && docker compose up -d keycloak`.

### Biến môi trường mới (đã có trong `.env` / `.env.example` mỗi module)

| Biến | Mặc định | Dùng ở |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/mss301` | chỉ gateway |

### Tài khoản demo (mật khẩu `123456`)

| Username | userId (= `sub` của token) | Role |
|---|---|---|
| `customer` | `10000000-0000-0000-0000-000000000001` | CUSTOMER |
| `staff` | `10000000-0000-0000-0000-000000000002` | STAFF |
| `blocked` | `10000000-0000-0000-0000-000000000003` | CUSTOMER (bị khoá trong user-service) |
| `admin` | `10000000-0000-0000-0000-000000000004` | ADMIN |

Lấy token để test:

```bash
curl -X POST http://localhost:8180/realms/mss301/protocol/openid-connect/token \
  -d grant_type=password -d client_id=mss301-frontend -d username=customer -d password=123456
```

---

## 3. Xác thực bằng Keycloak

### 3.1 Vì sao Keycloak

Tự quản lý user, mật khẩu, role (không phụ thuộc Google); có sẵn trang đăng nhập / đăng ký / quên mật khẩu;
muốn "Đăng nhập bằng Google" chỉ cần bật Identity Provider trong Keycloak, backend không đổi.

Trước đây FE tự gửi `X-User-Id` / `X-User-Role`, ai cũng giả làm admin được. Giờ danh tính **chỉ** lấy từ token đã kiểm tra.

### 3.2 Gateway (`infras/api-gateway/.../security`)

| Class | Việc |
|---|---|
| `SecurityConfig` | Resource server JWT (WebFlux). Public: GET phim / rạp / phòng / suất chiếu, webhook PayOS, health, `/rate-limit/**`, OPTIONS. Còn lại cần token → `401` |
| `UserHeaderRelayFilter` | **Luôn xoá** `X-User-Id`, `X-User-Role`, `X-User-Email`, `X-User-Name` client gửi; nếu có token thì gắn lại từ token: `sub`, role cao nhất (ADMIN > STAFF > CUSTOMER), email, tên (URL-encoded) |
| `JsonSecurityErrors` | 401 / 403 dạng JSON tiếng Việt (mặc định Spring trả body rỗng) |

Khoá công khai của realm được tải khi có token đầu tiên, nên gateway khởi động được dù Keycloak chưa chạy.

### 3.3 Từng service (downstream): header → SecurityContextHolder

Token chỉ được kiểm tra **một lần ở Gateway**. Gateway gửi danh tính xuống bằng header (`X-User-Id` = `sub`,
`X-User-Role` = role cao nhất, `X-User-Email`, `X-User-Name`); header giả của client đã bị Gateway xoá.
Service **không** cần Keycloak, chỉ đọc header và đưa vào **`SecurityContextHolder`** (gói `infrastructure/security`):

| Class | Việc |
|---|---|
| `GatewayHeaderAuthenticationFilter` | `X-User-Id` + `X-User-Role` → `UsernamePasswordAuthenticationToken(principal = userId, authority = ROLE_<role>)` → `SecurityContextHolder`. Không có / sai `X-User-Id` → anonymous. Role lạ → CUSTOMER |
| `SecurityConfig` | Spring Security stateless (không còn resource server). Public: đường public của service + `/internal/**`; còn lại `authenticated()` → thiếu header thì `401` |
| `CurrentUser` | Đọc người gọi từ `SecurityContextHolder`: `id()`, `role()`, `hasRole("ADMIN")` |
| `AdminAccess` (movie, cinema, showtime) / `requireAdmin()` (user) | Kiểm tra quyền admin bằng `CurrentUser.hasRole("ADMIN")` (trước đây đọc header trực tiếp) |
| `JsonSecurityErrors` | 401 / 403 JSON tiếng Việt |

```java
// GatewayHeaderAuthenticationFilter
UUID userId = UUID.fromString(request.getHeader("X-User-Id"));
String role = request.getHeader("X-User-Role");                 // ADMIN / STAFF / CUSTOMER
var auth = UsernamePasswordAuthenticationToken.authenticated(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
SecurityContextHolder.getContext().setAuthentication(auth);

// AdminAccess
if (!CurrentUser.hasRole("ADMIN")) throw new ForbiddenException("Chỉ ADMIN mới được quản lý phim");
```

Controller vẫn nhận `@RequestHeader("X-User-Id")` để lấy id người gọi (cùng giá trị với principal).

### 3.4 Hồ sơ user

`GET /api/v1/users/me`: lần đầu đăng nhập sẽ **tạo hồ sơ** trong user-service từ token (id = `sub`, email, tên, role);
các lần sau cập nhật role theo Keycloak. FE phải gọi API này ngay sau khi đăng nhập, vì booking kiểm tra user trong
user-service trước khi đặt vé. Khoá tài khoản: admin `PATCH /api/v1/users/{id}/status` → `BLOCKED` (vẫn đăng nhập được
nhưng không đặt vé được).

### 3.5 Giới hạn

- Service **tin header của Gateway**: gọi thẳng cổng service và tự gửi `X-User-Role: ADMIN` thì giả được. Khi deploy
  chỉ mở cổng Gateway (các service nằm trong mạng nội bộ / docker network).
- `/internal/**` không cần đăng nhập (chỉ service gọi nhau).
- Token đã cấp còn hiệu lực tới khi hết hạn (30 phút) dù user bị tắt trong Keycloak.

---

## 4. Interceptor và Header Propagation trong OpenFeign

### 4.1 Ý tưởng

Một thao tác của user đi qua nhiều service. Gateway chỉ gắn header cho service **đầu tiên** (ví dụ booking). Muốn
service phía sau (showtime, user) **biết cùng một user** và **log của mọi service ghép được với nhau**, header phải
được chuyển tiếp ở **mỗi bước gọi**. Với OpenFeign, chỗ làm việc đó là
`RequestInterceptor`: chạy trước **mọi** request Feign, thêm header vào `RequestTemplate`.

### 4.2 Các thành phần

| Ở đâu | Class | Việc |
|---|---|---|
| Gateway | `web/CorrelationIdWebFilter` | Giữ `X-Correlation-Id` hợp lệ của client hoặc tạo UUID; gửi xuống service và trả lại trong response (kể cả 401 / 403) |
| Mọi service | `infrastructure/web/CorrelationIdFilter` | Đọc (hoặc tạo) ID, đưa vào **MDC** `correlationId`, trả lại trong response; mỗi dòng log có `[cid=...]` (`logging.pattern.correlation`) |
| booking, showtime | `infrastructure/client/FeignHeaderPropagationConfig` | Bean `RequestInterceptor` áp cho **mọi** Feign client |

```java
@Bean
public RequestInterceptor headerPropagationInterceptor() {
    return template -> {
        // 1. Correlation ID của request hiện tại (MDC); gọi từ Kafka / job thì tạo mới
        String cid = MDC.get("correlationId");
        template.header("X-Correlation-Id", cid != null ? cid : UUID.randomUUID().toString());
        // 2. Người gọi: lấy từ SecurityContextHolder (do GatewayHeaderAuthenticationFilter đặt)
        CurrentUser.id().ifPresent(id -> template.header("X-User-Id", id.toString()));
        CurrentUser.role().ifPresent(role -> template.header("X-User-Role", role));
        // 3. Email / tên: copy từ request hiện tại (user-service dùng khi tạo hồ sơ)
        copyFromCurrentRequest(template, "X-User-Email", "X-User-Name");
    };
}
```

### 4.3 Hành vi

| Tình huống | `X-User-Id` / `X-User-Role` gửi đi | `X-Correlation-Id` gửi đi |
|---|---|---|
| Trong request của user (đặt vé, admin tạo suất chiếu) | Người đang gọi (từ `SecurityContextHolder`) | ID của request (từ Gateway) |
| Ngoài request (Kafka listener xác nhận ghế, job hết hạn booking) | Không có | UUID mới |

- `SecurityContextHolder` / `RequestContextHolder` gắn với **thread**; Feign chạy cùng thread với request
  (Resilience4j `decorateSupplier` cũng vậy) nên đọc được. Gọi Feign trong thread khác (async) thì phải tự copy.
- Service nhận dựng lại `SecurityContextHolder` từ các header này, nên `CurrentUser` ở showtime / user cho cùng người gọi.
- Correlation ID chưa đi qua Kafka (booking → payment mỗi bên có ID riêng).

Test: `FeignHeaderPropagationConfigTest`, `FeignContractPropagationTest` (booking) – dựng Feign client thật, gọi vào
HTTP server trong test và kiểm tra header server nhận được.

---

## 5. Feign client trong `infrastructure/client` của từng service

### 5.1 Trước và sau

Trước: module `contracts/internal-api` (jar dùng chung) chứa interface `XxxInternalApi` + DTO; controller `implements`,
Feign client `extends`. Các service dùng chung code nên không còn độc lập, vi phạm bounded context của DDD.
Module này **đã xoá**.

Sau: mỗi service tự sở hữu phía của mình.

```java
// booking-service (bên gọi): infrastructure/client/ShowtimeFeignClient.java
@FeignClient(name = "showtime-service", url = "${clients.showtime.base-url:}")
public interface ShowtimeFeignClient {
    @PostMapping("/internal/holds/{id}/release")
    RemoteResponse<SeatHoldResponse> releaseHold(@PathVariable("id") UUID holdId); // DTO trong infrastructure/client/dto
    ...
}

// showtime-service (bên cung cấp): api/rest/InternalShowtimeController.java
@RestController
public class InternalShowtimeController {
    @PostMapping("/internal/holds/{id}/release")
    public ApiResponse<SeatHoldResponse> releaseHold(@PathVariable("id") UUID holdId) { ... } // DTO trong api/dto
}
```

| Cặp gọi | Feign client (bên gọi, `infrastructure/client`) | Adapter (bên gọi) | Controller (bên cung cấp) |
|---|---|---|---|
| booking → showtime | `ShowtimeFeignClient` | `ShowtimeClientAdapter` | `InternalShowtimeController` |
| booking → user | `UserFeignClient` | `UserClientAdapter` | `InternalUserController` |
| booking → payment | `PaymentFeignClient` | `PaymentStatusAdapter` | `InternalPaymentController` |
| showtime → cinema | `CinemaFeignClient` | `CinemaClientAdapter` | `InternalCinemaController` |
| showtime → movie | `MovieFeignClient` | `MovieClientAdapter` | `InternalMovieController` |

### 5.2 Quy tắc

- Feign client + DTO của bên gọi nằm trong `infrastructure/client` (`dto/` gồm cả envelope `RemoteResponse<T>`).
  Chỉ adapter dùng chúng; `application` / `domain` chỉ thấy port và kiểu của chính service.
- Controller bên cung cấp tự khai báo mapping, dùng `ApiResponse` + DTO trong `api/dto` của chính nó.
- Đường dẫn **đầy đủ trên từng method**; không đặt `@RequestMapping` mức interface trên Feign client.
- Ghi rõ tên `@PathVariable("id")`.
- DTO dùng `String` cho trạng thái, không dùng enum domain của service khác.
- Đổi endpoint nội bộ phải sửa cả hai phía; giữ tương thích ngược và dựa vào test
  (`FeignContractPropagationTest`). Chi tiết: `rule/fix/feign_client_per_service.md`.

---

## 6. Xử lý lỗi

### 6.1 Body lỗi chung

```json
{ "success": false, "status": 409, "error": "Conflict",
  "message": "Ghế A3 vừa có người khác chọn", "path": "/api/v1/bookings",
  "errors": { "field": "thông báo" }, "timestamp": "..." }
```

Mọi `message` / `errors` bằng **tiếng Việt**, hiển thị thẳng cho người dùng. Lỗi 500 không lộ chi tiết kỹ thuật
("Hệ thống đang gặp sự cố, vui lòng thử lại sau"); chi tiết nằm trong log (kèm `[cid=...]`).

| Nguồn | Xử lý |
|---|---|
| Validate DTO (`@NotNull(message=...)`...) | Message tiếng Việt ở cả 6 service |
| Lỗi nghiệp vụ (domain / application) | Câu tiếng Việt, kể cả câu ghép ("Ghế " + nhãn + " vừa có người khác chọn") |
| Lỗi framework | `CommonExceptionHandler` mỗi service: JSON sai, sai kiểu tham số, thiếu tham số / header, sai method, sai Content-Type, không có đường dẫn, optimistic lock (409), vi phạm ràng buộc DB (409) |
| 401 / 403 | `JsonSecurityErrors` (gateway + service) |
| Lỗi của Gateway | `GatewayErrorHandler`: 404 không có route, 503 service không kết nối được, 504 quá thời gian; rate limit 429 |

### 6.2 Lỗi Feign

`FeignErrors.message(e)` đọc trường `message` trong body lỗi của service bên kia, nên câu tiếng Việt gốc được trả tiếp
cho FE.

| Service bên kia trả | booking | showtime (gọi cinema / movie) |
|---|---|---|
| 409 / 400 / 404 khi giữ ghế | Giữ status + câu của showtime | – |
| 404 user | 400 "Chưa có hồ sơ người dùng, hãy mở trang cá nhân..." | – |
| 404 phim / rạp / phòng | – | 400 "Không tìm thấy phim / rạp / phòng chiếu ..." |
| 401 / 403 | Giữ status + câu của service kia | 400 + câu của service kia |
| Sập / timeout / 5xx | 503 "Dịch vụ ... tạm thời không khả dụng" + `Retry-After` | 503 |

---

## 7. Resilience4j trong luồng booking

### 7.1 Bài toán

booking gọi đồng bộ sang user (`validateUser`) và showtime (`holdSeats`, `confirmHold`, `releaseHold`).
Không bảo vệ thì showtime **sập** làm mọi request chờ timeout; showtime **chậm** chiếm hết thread + kết nối DB của
booking → booking chết theo (lỗi dây chuyền); showtime vừa khởi động lại bị dội ngay.

### 7.2 Ba lớp

```
Retry ( CircuitBreaker ( Bulkhead ( Feign call ) ) )        + timeout Feign: connect 2s, read 5s (user 3s)
```

| Lớp | Cấu hình | Giải quyết |
|---|---|---|
| **Retry** | 3 lần, 300ms → 600ms | Lỗi thoáng qua: connection refused, 502/503/504. **Không** retry read timeout (service chậm), 4xx, breaker OPEN, bulkhead đầy. An toàn vì mọi lời gọi idempotent (giữ ghế khoá theo `bookingId`) |
| **Circuit Breaker** | 10 lần gần nhất, tối thiểu 5, ≥ 50% lỗi → OPEN 20s → HALF_OPEN thử 3 lần | Service hỏng hẳn: trả 503 ngay, không gọi nữa, tự hồi phục |
| **Bulkhead** | 20 lời gọi cùng lúc / service, chờ tối đa 500ms | Service chậm không chiếm hết thread; hết chỗ → 503 "đang quá tải" |

```
          lỗi kỹ thuật ≥ 50%                          sau 20s
 CLOSED ───────────────────────▶ OPEN ───────────────────────▶ HALF_OPEN
   ▲                             (503 ngay,                       │
   └──────── 3 lần thử đều OK ── không gọi) ◀──── có lỗi ─────────┘
```

Breaker chỉ đếm **lỗi kỹ thuật** (không kết nối được, timeout, 5xx). 409 "ghế đã có người giữ" là câu trả lời nghiệp
vụ, đếm là thành công; nếu không, giờ cao điểm tranh ghế sẽ tự khoá hệ thống. Bulkhead đầy không được đếm.
Hai bộ riêng `showtime` và `user`: lỗi được cô lập theo service.

### 7.3 Ảnh hưởng theo bước

| Bước | Khi showtime / user lỗi |
|---|---|
| Đặt vé | Lỗi thoáng qua: retry, người dùng không thấy. Hỏng / OPEN / quá tải: 503, không tạo booking |
| Xác nhận ghế sau thanh toán (Kafka) | Booking giữ `CONFIRMING`, job thử lại mỗi 60s; quá 30 phút → `CANCELLED`, log `MANUAL CHECK` |
| Huỷ / hết hạn booking | Nhả ghế kiểu best effort (chỉ log) |

### 7.4 Code

`services/booking-service/src/main/java/com/mv/bookingservice/infrastructure/resilience/`:
`ResilienceConfig` (3 registry, phân loại lỗi), `RemoteCallGuard` (bộ 3 lớp của một service), `ResilientCall`
(ghép lớp, đổi lỗi thành 503 tiếng Việt), `*Properties` (cấu hình `booking.resilience.*`),
`CircuitBreakerStatusController` (`GET http://localhost:8083/internal/resilience` để xem trạng thái khi demo).

```java
// ShowtimeClientAdapter
return guard.call(() -> toSeatHoldResult(requireData(showtimeFeignClient.releaseHold(holdId), "release hold")));
```

Test: `ShowtimeClientAdapterCircuitBreakerTest`, `ShowtimeClientAdapterRetryBulkheadTest`.

---

## 8. Phạm vi nghiệp vụ đã chốt

- **Không có hoàn tiền.** Payment chỉ kết thúc ở `SUCCESS` / `FAILED` / `EXPIRED` / `CANCELLED`; booking ở
  `PENDING` / `CONFIRMING` / `CONFIRMED` / `CANCELLED` / `EXPIRED`. Khách chỉ huỷ được booking chưa thanh toán; suất
  chiếu đã có người giữ / mua ghế không sửa / huỷ được. Trường hợp đã nhận tiền mà không dùng được chỉ ghi log
  `MANUAL CHECK` để xử lý tay.
- **Không dùng service token** (client credentials) cho lời gọi nội bộ: token kiểm tra một lần ở Gateway, danh tính
  đi tiếp bằng header (mục 3.3, 4).

---

## 9. Demo nhanh

| # | Demo | Cách làm | Thấy gì |
|---|---|---|---|
| 1 | Chưa đăng nhập | `GET http://localhost:8080/api/v1/bookings` không token | 401 JSON "Bạn cần đăng nhập..." |
| 2 | Không giả được admin | Token `customer` + header `X-User-Role: ADMIN`, `POST /api/v1/movies` | 403 (header giả bị xoá) |
| 3 | SecurityContext ở service | `GET http://localhost:8083/api/v1/bookings` không header → 401; thêm `X-User-Id` của customer → 200 (vì vậy chỉ được mở cổng Gateway) | Service dựng người gọi từ header |
| 4 | Correlation ID | Đặt vé kèm `X-Correlation-Id: demo-001` | Response có header đó; log booking / showtime / user cùng `[cid=demo-001]` |
| 5 | Header propagation | Đặt vé bằng token `customer` | showtime / user nhận `X-User-Id` của customer qua Feign interceptor |
| 6 | Lỗi tiếng Việt | Đặt vé với `showtimeId: "abc"` | 400 "Dữ liệu gửi lên không đúng định dạng..." |
| 7 | Retry + Circuit Breaker | Stop showtime, đặt vé vài lần, xem `GET :8083/internal/resilience` và log `RETRY` / `CIRCUIT BREAKER`; start lại showtime | 503 → OPEN (503 ngay, không gọi) → sau 20s HALF_OPEN → 3 lần OK → CLOSED |
| 8 | Lỗi nghiệp vụ không mở breaker | Đặt ghế đã có người giữ 10 lần | 409 mỗi lần, breaker vẫn CLOSED, không retry |
| 9 | Bulkhead | Đặt `max-concurrent-calls: 1`, `max-wait-millis: 0`, gửi 10 request song song | Một số 503 "đang quá tải", breaker không đổi |
