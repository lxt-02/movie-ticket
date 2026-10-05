# Schema và luồng đặt vé xem phim

_Booking • Payment • Showtime_

Tài liệu triển khai database per service, gồm schema V1 của sáu service, migration V2 cho giữ ghế và thanh toán, contract API đồng bộ bằng Feign và Saga bất đồng bộ qua Kafka.

Booking gọi Showtime để kiểm tra và giữ ghế nguyên tử. Showtime sở hữu trạng thái ghế theo suất chiếu. Sau thanh toán, Booking chỉ xác nhận vé khi Showtime đã chuyển ghế sang BOOKED.

## 1 Phạm vi và cấu trúc

| Phần | Nội dung |
| --- | --- |
| 2 | Trách nhiệm service và các cuộc gọi Feign |
| 3 | Migration V2 và hướng dẫn Saga đầy đủ |
| 4 | SQL V1 của User, Movie, Cinema, Showtime, Booking, Payment |
| 5 | SQL V2 của Showtime, Booking, Payment |

Cách chạy: mỗi service dùng database riêng. Chạy V1 trong DB tương ứng, sau đó V2 nếu service có migration này. Các khóa ngoại của V2 chỉ nằm trong Showtime DB. Không chạy toàn bộ SQL của tài liệu vào cùng một database.

## 2 Trách nhiệm service và Feign

| Bên gọi | Bên nhận | Khi nào và mục đích |
| --- | --- | --- |
| Booking | Showtime | Khi đặt vé: kiểm tra suất, ghế, giá và giữ ghế. |
| Booking | User | Tùy yêu cầu kiểm tra trạng thái tài khoản mới nhất; userId lấy từ JWT đã xác thực. |
| Showtime | Movie | Khi tạo suất: xác minh phim và lấy thông tin phim. |
| Showtime | Cinema | Khi tạo suất: xác minh rạp/phòng, lấy layout và loại ghế. |
| Payment | Booking | Khi tạo thanh toán: kiểm tra quyền, trạng thái, thời hạn và lấy số tiền. |

Khi giữ ghế, Showtime kiểm tra DB của mình và snapshot đã đồng bộ. Không bắt buộc gọi Movie/Cinema mỗi lần nếu chấp nhận độ trễ của Kafka. Nếu cần kiểm tra trạng thái nguồn ngay lúc đặt, gọi hai service trước transaction giữ ghế; các cuộc gọi này vẫn không tạo transaction phân tán.

Các endpoint và message trong phần hướng dẫn là contract đề xuất để triển khai, không phải xác nhận rằng API hoặc consumer đã tồn tại. Migration chỉ tạo cấu trúc lưu trữ.

## 3 Migration V2 và hướng dẫn Saga

| Database | Bổ sung chính |
| --- | --- |
| showtime_db | seat_holds, seat_hold_items, hold_id, inbox_events, outbox_events và index. |
| booking_db | hold_id, idempotency_key, request_hash, currency, paid_payment_id, version, CONFIRMING và REFUND_PENDING. |
| payment_db | Idempotency cho payment/refund, expires_at, REFUND_PENDING và index. |

### Áp dụng schema

Chạy V1 rồi V2 của từng thư mục trong database tương ứng. Không chạy cả ba V2 trên cùng DB. Migration chỉ bổ sung cấu trúc; ứng dụng phải triển khai transaction, API và consumer bên dưới.

V2 không sửa migration V1. Các cột mới của booking/payment/refund cho phép NULL để giữ dữ liệu lịch sử; API tạo mới phải yêu cầu idempotency key và lưu hash của request đã chuẩn hóa. Cùng key, khác payload phải trả 409; cùng key, cùng payload trả tài nguyên cũ. Không tái sử dụng bookingId cho lượt giữ ghế mới.

Constraint ck_seat_hold_ownership là NOT VALID để không đoán chủ ghế HELD/BOOKED cũ. Trước khi bật luồng mới, đối soát các hàng cũ, gán đúng hold cho ghế đã bán, xử lý các hold hết hạn rồi chạy ALTER TABLE showtime_seats VALIDATE CONSTRAINT ck_seat_hold_ownership;. DB mới có thể validate ngay. Không tự chuyển ghế BOOKED cũ về AVAILABLE.

### Showtime gọi service nào

Khi tạo suất chiếu, Showtime gọi Movie để kiểm tra phim, thời lượng, thông tin phân loại; gọi Cinema để kiểm tra rạp/phòng đang hoạt động và lấy layout/loại ghế. Sau đó lưu snapshot và tạo showtime_seats. Giá bán cuối cùng theo suất thuộc Showtime.

Khi Booking gọi giữ ghế, Showtime kiểm tra DB nội bộ: suất đang bán, thời hạn đặt, snapshot hợp lệ, đúng ghế/đúng suất và khả dụng. Không cần gọi tiếp User, Payment, Booking; không cần gọi Movie/Cinema trên mọi lần giữ ghế nếu nhóm chấp nhận snapshot có độ trễ.

Movie/Cinema cần phát event cho cả thay đổi trạng thái, xóa/vô hiệu hóa, không chỉ tạo mới. Showtime tiêu thụ theo version, bỏ event cũ, cập nhật snapshot và chặn bán suất bị ảnh hưởng. Schema hiện chỉ có status phòng trong screen_snapshots; trạng thái rạp cần event ánh xạ sang vô hiệu hóa suất hoặc bổ sung cinema snapshot riêng khi triển khai. Không ghi đè layout của suất đã có hold/vé; thay đổi phải có chính sách chuyển/hủy/hoàn tiền.

Nếu yêu cầu kiểm tra trạng thái nguồn ngay tại thời điểm đặt: Showtime gọi Movie và Cinema trước transaction giữ ghế; lỗi/timeout thì từ chối tạm thời. Các cuộc gọi đó không tạo transaction phân tán, vẫn có khoảng đua nếu nguồn thay đổi sau response. Muốn bảo đảm chặt phải thống nhất quy trình vô hiệu hóa suất do Showtime điều phối. Snapshot thiếu thì từ chối tạm thời và đồng bộ lại; không mặc định hợp lệ.

### API nội bộ đề xuất

- Showtime: POST /internal/showtimes/{id}/holds, body bookingId + showtimeSeatIds, header Idempotency-Key. Trả holdId, expiresAt, danh sách ghế/giá và snapshot phim/rạp/phòng.

- Showtime: GET /internal/holds/by-booking/{bookingId} để tra cứu sau timeout.

- Showtime: confirm/release theo holdId bằng Kafka command hoặc API idempotent; chọn một đường xử lý nghiệp vụ chung.

- Booking: GET /internal/bookings/{id}/payment-context. Kiểm tra người sở hữu, trạng thái PENDING, chưa hết hạn; trả số tiền và currency từ server.

API nội bộ phải xác thực service gọi. userId lấy từ danh tính đã xác thực; Payment kiểm tra người yêu cầu có quyền thanh toán booking.

### Transaction giữ ghế

1. Booking cấp bookingId và idempotency key ổn định trước khi gọi Showtime. Retry phải giữ nguyên ID/key; không tạo lượt giữ mới sau timeout.

2. Showtime khóa hàng showtime và các ghế theo thứ tự ID cố định. Luồng vô hiệu hóa suất cũng phải phối hợp khóa hàng showtime này.

3. Kiểm tra toàn bộ ghế tồn tại, không trùng, cùng suất và AVAILABLE. Ghế HELD hết hạn chỉ được tái cấp sau khi đã kết thúc hold cũ bằng cùng cơ chế khóa.

4. Trong cùng transaction: tạo seat_holds, seat_hold_items; cập nhật mọi ghế sang HELD, hold_id, locked_until. Hoặc thành công tất cả hoặc rollback tất cả. Trùng key thì lấy hold cũ và đối chiếu hash.

5. Booking lưu PENDING và snapshot từ response, expires_at bằng expiresAt của hold. Lưu thất bại thì release đúng hold; job hết hạn là dự phòng. Nếu Booking chết trước khi lưu, hold tự hết hạn.

Không giữ transaction DB mở trong 10 phút hoặc trong khi gọi HTTP. updated_at và version không tự tăng; application phải cập nhật. API booking cần lưu/recover mapping request -> bookingId ổn định để retry sau crash không tạo hold mới.

### Saga qua Kafka

Payment xác minh webhook (chữ ký, transaction, amount, currency), lưu kết quả và outbox trong cùng transaction. PaymentSucceeded chứa eventId, bookingId, paymentId, amount, currency. Không tin redirect của FE.

Booking nhận PaymentSucceeded, kiểm tra amount/currency và paymentId đã xử lý; khóa booking hoặc dùng optimistic version. PENDING -> CONFIRMING và ghi ConfirmSeatsRequested vào outbox. Nếu booking đã hết hạn/hủy, yêu cầu refund; nếu một giao dịch khác đã thanh toán, refund giao dịch dư, không hủy vé hợp lệ.

Showtime nhận ConfirmSeatsRequested: khóa hold và ghế, chỉ xác nhận đúng booking/hold còn HELD, chưa hết hạn, toàn bộ ghế vẫn thuộc hold và suất còn cho phép. Thành công chuyển hold CONFIRMED, ghế BOOKED, xóa locked_until nhưng giữ hold_id; phát SeatsBooked. Hold đã CONFIRMED thì retry trả thành công. Thất bại phát SeatConfirmationFailed.

Booking nhận SeatsBooked: CONFIRMING -> CONFIRMED, sau đó mới cho phép sử dụng ticket_code. SeatConfirmationFailed sau thu tiền: REFUND_PENDING + RefundRequested. PaymentRefunded cập nhật REFUNDED khi phù hợp; refund giao dịch dư không đổi booking CONFIRMED thành REFUNDED.

Job Booking hết hạn chỉ chuyển PENDING -> EXPIRED và phát BookingExpired. Job Showtime hết hạn hold dùng cùng khóa/điều kiện với confirm: chỉ HELD quá hạn mới EXPIRED và trả ghế. Booking ở CONFIRMING quá lâu cần đối soát hold/payment, không tự coi là thất bại. Kafka đến muộn khi hold đã hết hạn thì refund dù provider thu tiền trước hạn.

Release chỉ áp dụng hold HELD, đúng hold_id; không giải phóng ghế BOOKED hoặc ghế thuộc hold mới. Hủy vé đã xác nhận cần nghiệp vụ riêng. PaymentFailed của một attempt không tự hủy booking nếu còn được thử thanh toán lại.

Inbox insert, cập nhật nghiệp vụ và ghi outbox phải chung transaction. Outbox relay gửi rồi mới đánh dấu published; vẫn có thể gửi lặp. Dùng bookingId làm Kafka key, nhưng không giả định có thứ tự giữa các topic. Consumer kiểm tra trạng thái hiện tại, retry lỗi tạm thời, DLQ và đối soát. Refund cần key ổn định cả ở DB lẫn cổng thanh toán; timeout phải tra cứu, không lập tức hoàn lần hai. Tổng refund thành công/đang xử lý không được vượt số tiền đã thu (kiểm tra dưới khóa payment).

SeatsHeld nếu được phát chỉ phục vụ đồng bộ/quan sát trong thiết kế giữ ghế đồng bộ này; không để consumer tạo booking lần hai.

## 4 SQL schema V1 đầy đủ

Sao chép từng script và chạy riêng trong database ghi tại tiêu đề. Nội dung SQL dưới đây được giữ nguyên từ file migration; dòng dài có thể tự xuống dòng khi hiển thị trong Word.

### User database

File: user_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    avatar_url TEXT,
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN')),
    CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth_accounts_user ON oauth_accounts(user_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

### Movie database

File: movie_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    description TEXT,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    release_date DATE,
    age_rating VARCHAR(20),
    language VARCHAR(50),
    country VARCHAR(100),
    poster_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'COMING_SOON',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED', 'ARCHIVED'))
);

CREATE TABLE genres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL,
    genre_id UUID NOT NULL,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    avatar_url TEXT,
    biography TEXT
);

CREATE TABLE movie_crew (
    movie_id UUID NOT NULL,
    person_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    character_name VARCHAR(150),
    PRIMARY KEY (movie_id, person_id, role),
    CHECK (role IN ('DIRECTOR', 'ACTOR'))
);

CREATE TABLE trailers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    title VARCHAR(255),
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL, payload JSONB NOT NULL,
    published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Cinema database

File: cinema_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE cinemas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(255) NOT NULL,
    brand VARCHAR(100), address TEXT NOT NULL, city VARCHAR(100) NOT NULL,
    district VARCHAR(100), phone VARCHAR(30), status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), cinema_id UUID NOT NULL,
    screen_type_id UUID NOT NULL, name VARCHAR(100) NOT NULL, total_seats INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', UNIQUE (cinema_id, name)
);

CREATE TABLE seat_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50) NOT NULL UNIQUE,
    price_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00 CHECK (price_multiplier > 0)
);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), screen_id UUID NOT NULL,
    seat_type_id UUID NOT NULL, row_label VARCHAR(5) NOT NULL, seat_number INT NOT NULL,
    label VARCHAR(10) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (screen_id, label), UNIQUE (screen_id, row_label, seat_number)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL, payload JSONB NOT NULL,
    published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Showtime database

File: showtime_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Snapshots are deliberately local copies; no FK points to movie_db/cinema_db.
CREATE TABLE movie_snapshots (
    movie_id UUID PRIMARY KEY, title VARCHAR(255) NOT NULL, duration_minutes INT NOT NULL,
    age_rating VARCHAR(20), status VARCHAR(30), version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screen_snapshots (
    screen_id UUID PRIMARY KEY, cinema_id UUID NOT NULL, cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL, screen_type VARCHAR(50), total_seats INT NOT NULL,
    status VARCHAR(30), version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE showtimes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), movie_id UUID NOT NULL, screen_id UUID NOT NULL,
    movie_title VARCHAR(255) NOT NULL, cinema_id UUID NOT NULL, cinema_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL, start_time TIMESTAMPTZ NOT NULL, end_time TIMESTAMPTZ NOT NULL,
    base_price NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_time > start_time)
);

CREATE TABLE showtime_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), showtime_id UUID NOT NULL, seat_id UUID NOT NULL,
    seat_label VARCHAR(10) NOT NULL, seat_type VARCHAR(50), price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', locked_until TIMESTAMPTZ,
    UNIQUE (showtime_id, seat_id),
    CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED'))
);

CREATE INDEX idx_showtimes_movie_time ON showtimes(movie_id, start_time);
CREATE INDEX idx_showtime_seats_status ON showtime_seats(showtime_id, status);
```

### Booking database

File: booking_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id UUID NOT NULL, showtime_id UUID NOT NULL, movie_title VARCHAR(255) NOT NULL,
    cinema_name VARCHAR(255) NOT NULL, screen_name VARCHAR(100) NOT NULL, start_time TIMESTAMPTZ NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0, discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0, status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ,
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED'))
);

CREATE TABLE booking_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), booking_id UUID NOT NULL,
    showtime_seat_id UUID NOT NULL, seat_id UUID NOT NULL, seat_label VARCHAR(10) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0), ticket_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', UNIQUE (booking_id, showtime_seat_id)
);

CREATE TABLE inbox_events (
    event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL, payload JSONB NOT NULL,
    published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);
CREATE INDEX idx_bookings_expiry ON bookings(status, expires_at);
```

### Payment database

File: payment_db/V1__init.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), booking_id UUID NOT NULL, user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL, provider_transaction_id VARCHAR(150) UNIQUE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0), currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', payment_url TEXT, paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), payment_id UUID NOT NULL,
    provider_event_id VARCHAR(150) NOT NULL UNIQUE, event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), payment_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0), reason TEXT,
    provider_refund_id VARCHAR(150) UNIQUE, status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMPTZ
);

CREATE TABLE inbox_events (
    event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL, payload JSONB NOT NULL,
    published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 5 SQL migration V2 đầy đủ

Sao chép từng script và chạy riêng trong database ghi tại tiêu đề. Nội dung SQL dưới đây được giữ nguyên từ file migration; dòng dài có thể tự xuống dòng khi hiển thị trong Word.

### Showtime database

File: showtime_db/V2__seat_holds_and_saga.sql

```sql
-- Apply after V1, in showtime_db only. Run the whole file in one transaction.
BEGIN;

CREATE TABLE seat_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE,
    showtime_id UUID NOT NULL REFERENCES showtimes(id),
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD'
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (expires_at > created_at),
    UNIQUE (id, showtime_id)
);

ALTER TABLE showtime_seats ADD COLUMN hold_id UUID;
ALTER TABLE showtime_seats ADD CONSTRAINT fk_seat_hold_showtime
    FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id);
-- NOT VALID preserves legacy HELD/BOOKED rows for explicit reconciliation.
-- PostgreSQL still enforces this check for all newly inserted/updated rows.
ALTER TABLE showtime_seats ADD CONSTRAINT ck_seat_hold_ownership CHECK (
    (status = 'HELD' AND hold_id IS NOT NULL AND locked_until IS NOT NULL)
    OR (status = 'BOOKED' AND hold_id IS NOT NULL AND locked_until IS NULL)
    OR (status IN ('AVAILABLE', 'BLOCKED') AND hold_id IS NULL AND locked_until IS NULL)
) NOT VALID;

-- Keeps the original seats/prices for idempotent responses after release.
ALTER TABLE showtime_seats ADD CONSTRAINT uq_showtime_seat_identity UNIQUE (id, showtime_id);
CREATE TABLE seat_hold_items (
    hold_id UUID NOT NULL,
    showtime_id UUID NOT NULL,
    showtime_seat_id UUID NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (hold_id, showtime_seat_id),
    FOREIGN KEY (hold_id, showtime_id) REFERENCES seat_holds(id, showtime_id),
    FOREIGN KEY (showtime_seat_id, showtime_id) REFERENCES showtime_seats(id, showtime_id)
);

CREATE INDEX idx_seat_holds_expiry ON seat_holds(expires_at) WHERE status = 'HELD';
CREATE INDEX idx_showtime_seats_hold ON showtime_seats(hold_id) WHERE hold_id IS NOT NULL;

CREATE TABLE inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_showtime_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
COMMIT;
```

### Booking database

File: booking_db/V2__booking_saga.sql

```sql
BEGIN;
ALTER TABLE bookings
    ADD COLUMN hold_id UUID UNIQUE,
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    ADD COLUMN paid_payment_id UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uq_booking_user_request UNIQUE (user_id, idempotency_key),
    ADD CONSTRAINT ck_booking_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );

ALTER TABLE bookings DROP CONSTRAINT bookings_status_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_status_check CHECK (
    status IN ('PENDING', 'CONFIRMING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUND_PENDING', 'REFUNDED')
);
CREATE INDEX idx_booking_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
-- New bookings must populate hold_id, idempotency_key and request_hash in the app.
-- Columns remain nullable for historical bookings; no cross-database FK is used.
COMMIT;
```

### Payment database

File: payment_db/V2__payment_idempotency.sql

```sql
BEGIN;
ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uq_payment_user_request UNIQUE (user_id, idempotency_key),
    ADD CONSTRAINT ck_payment_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );
ALTER TABLE payments DROP CONSTRAINT payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check CHECK (
    status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUND_PENDING', 'REFUNDED')
);
-- Allow multiple attempts: a late provider success must still be recorded.
CREATE INDEX idx_payments_booking ON payments(booking_id, created_at);

ALTER TABLE refunds
    ADD COLUMN idempotency_key VARCHAR(150),
    ADD COLUMN request_hash VARCHAR(64),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uq_refund_request UNIQUE (payment_id, idempotency_key),
    ADD CONSTRAINT ck_refund_request_pair CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    );
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_payment_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
COMMIT;
```
