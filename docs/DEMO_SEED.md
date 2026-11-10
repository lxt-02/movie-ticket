# Dữ liệu mẫu & kịch bản demo luồng đặt vé

Seed nằm trong Flyway của từng service (`V2__seed_data.sql`, payment-service `V3__seed_data.sql`).
**Giờ chiếu tính theo thời điểm migrate** (giờ Việt Nam), nên mỗi lần dựng lại DB đều có suất cho hôm nay → 9 ngày tới.

## 1. Dựng lại

```bash
docker compose --profile app down -v          # BẮT BUỘC xoá volume: V2 đã đổi nội dung (Flyway checksum) và giờ chiếu tính lại
docker compose --profile app up -d --build
cd frontend && npm install && npm run dev      # http://localhost:3000
```

Nên dựng lại **trong vòng 1–2 ngày trước buổi demo**: suất mẫu `50000000-…-0001` (dùng trong Postman) ở 20:00 *ngày kia*
so với lúc migrate, lưới suất chiếu kéo dài 10 ngày.

Thanh toán thật cần key PayOS sandbox trong `services/payment-service/.env` (xem `FRONTEND_API.md` §5).

## 2. Tài khoản (mật khẩu `123456`)

| Username | Role | Trạng thái | Dùng để demo |
|---|---|---|---|
| `customer` | CUSTOMER | ACTIVE | Đặt vé, thanh toán, xem lịch sử vé (có sẵn 4 đơn) |
| `staff` | STAFF | ACTIVE | Trang Vận hành (outbox, DLT); có 1 đơn bán 4 vé |
| `admin` | ADMIN | ACTIVE | Trang Quản trị |
| `blocked` | CUSTOMER | **BLOCKED** | Đăng nhập được, đặt vé bị chặn (403) |

user-service còn 6 hồ sơ phụ (không có tài khoản Keycloak) để danh sách người dùng của admin không trống.

## 3. Dữ liệu

**Phim** – 7 đang chiếu (Avatar: Dòng Chảy Của Nước, Dune – Phần Hai, Những Mảnh Ghép Cảm Xúc 2, Lật Mặt 7, Mai,
Doraemon: Bản Giao Hưởng Địa Cầu, Đào Phở và Piano), 1 đã chiếu (Godzilla x Kong), 2 sắp chiếu (phim hư cấu, ngày khởi
chiếu = ngày migrate + 21 / + 35). 10 thể loại tiếng Việt, đạo diễn & diễn viên → đủ để demo tìm kiếm / bộ lọc.

**Rạp & phòng**

| Rạp | Phòng | Loại | Ghế | Ghi chú |
|---|---|---|---|---|
| MV Cinema Landmark 81 (HCM) | Phòng 1 | 2D | 90 | **A8 hỏng** |
| | Phòng 2 | 3D | 55 | |
| | IMAX | IMAX | 133 | chỉ chiếu Avatar / Dune |
| MV Cinema Vincom Đồng Khởi (HCM) | Phòng 1 | 2D | 90 | C1, C2 hỏng |
| | Phòng 2 | 2D | 55 | **MAINTENANCE** → demo sửa sơ đồ ghế |
| MV Cinema Royal City (HN) | Phòng 1 | 2D | 90 | G12 hỏng |
| | Phòng 2 | 3D | 55 | |
| MV Cinema Đà Nẵng | Phòng 1 | 2D | 90 | rạp **INACTIVE** → không có suất |

Sơ đồ: hàng thường (STANDARD ×1) ở trước, VIP ×1,5 ở giữa, hàng cuối ghế đôi (COUPLE ×2).

**Suất chiếu** – mỗi phòng đang hoạt động có 4 suất/ngày: 09:30, 13:00, 16:30, 20:00, trong 10 ngày (bỏ các suất đã qua).
Giá cơ bản: 2D 80.000 · 3D 100.000 · IMAX 140.000; +10.000 suất 20:00; +10.000 thứ 7, CN. Trong 7 ngày tới: `SELLING`,
sau đó: `SCHEDULED`. Các suất cùng phòng luôn cách nhau ≥ 15 phút (admin tạo suất trùng giờ sẽ nhận 409).

**Suất cố định & lịch sử vé**

| | Suất | Ghế | Trạng thái | Thanh toán |
|---|---|---|---|---|
| `BK10000001` customer | Mai – Landmark P1, 3 ngày trước 19:30 (`50000000-…-0002`, ENDED) | D5 D6 | CONFIRMED | SUCCESS 240.000 |
| `BK10000002` customer | Dune – Landmark P1, ngày kia 20:00 (`50000000-…-0001`) | E7 E8 | CONFIRMED | SUCCESS 270.000 |
| `BK10000003` customer | như trên | A5 A6 | CANCELLED | CANCELLED |
| `BK10000004` customer | như trên | B1 B2 | EXPIRED | EXPIRED |
| `BK10000005` staff | như trên | G5–G8 | CONFIRMED | SUCCESS 540.000 |

Không seed booking `PENDING` nào → không có ghế bị giữ treo. Ghế của suất `…0001` có id kiểu cũ
`51000000-0000-0000-0000-0000000000NN` (A1=1 … A12=12, B1=13 …) để collection Postman trong `docs/` vẫn chạy.

## 4. Kịch bản demo (≈ 10 phút)

1. **Khách vãng lai** – trang chủ: tab Đang chiếu / Sắp chiếu → gõ "dune" ở ô tìm kiếm (gợi ý) → trang Tìm kiếm, lọc thể
   loại "Hành động". Mở phim **Dune** → lịch chiếu theo ngày, nhóm theo rạp.
2. Chọn suất **20:00 ngày kia, Landmark 81 – Phòng 1** → sơ đồ ghế: E7, E8, G5–G8 *đã bán*, A8 *không bán*.
   Chọn 2 ghế → nút "Đăng nhập để đặt vé".
3. **Đăng nhập `customer`** → quay lại trang ghế → **Đặt vé**: đơn `PENDING`, đồng hồ đếm ngược ~7 phút, chờ link PayOS.
4. Mở cửa sổ ẩn danh, đăng nhập `staff`, mở cùng suất → 2 ghế vừa chọn hiện *Đang có người giữ* và không chọn được (giữ chỗ 10 phút).
5. Quay lại `customer` → **Thanh toán qua PayOS** → quét QR sandbox → quay về `/payment/success` → "Đã nhận thanh toán,
   đang xác nhận ghế…" → **Đặt vé thành công** + mã vé từng ghế. Sơ đồ ghế: 2 ghế thành *Đã bán*.
6. **Vé của tôi** – thấy đủ các trạng thái: CONFIRMED (cũ + mới), CANCELLED, EXPIRED. Tra cứu mã `BK10000002`.
7. Nhánh phụ: đặt 1 đơn mới rồi **Huỷ đơn** (ghế trả lại ngay), hoặc bấm *Huỷ* trên trang PayOS → "Chưa thanh toán" →
   **Thanh toán lại** (tối đa 3 link).
8. Đăng nhập `blocked` → banner "tài khoản bị khoá", không đặt được vé.
9. **`admin`** – Quản trị: tạo suất cho phim đang chiếu (thử trùng giờ → 409), khoá / mở khoá user, rạp Vincom → Phòng 2
   (MAINTENANCE) → sửa sơ đồ ghế, đánh dấu ghế hỏng. **`staff`** – Vận hành: xem outbox / DLT.
