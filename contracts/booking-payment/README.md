# Hợp đồng booking-service ↔ payment-service

Nguồn sự thật duy nhất cho mọi message giữa hai service. Cả hai phía đều có test đọc các file ở đây:

- booking-service: `BookingPaymentContractTest`
- payment-service: `BookingPaymentContractTest`

| File | Kênh | Bên gửi → bên nhận |
|---|---|---|
| `contract.json` | Tên topic, danh sách `eventType`, mã lý do | cả hai |
| `create-payment.json` | Kafka `payment-commands` | booking → payment |
| `cancel-payment.json` | Kafka `payment-commands` | booking → payment |
| `payment-link-created.json` | Kafka `payment-events` | payment → booking |
| `payment-succeeded.json` | Kafka `payment-events` (cũng dùng cho FAILED / EXPIRED / CANCELLED, cùng payload) | payment → booking |
| `payment-create-failed.json` | Kafka `payment-events` | payment → booking |
| `paid-status.json` | HTTP `GET /internal/payments/by-booking/{bookingId}/paid-status` | payment trả cho booking |

Quy tắc: bên gửi phải sinh ra **đúng bộ trường** như file mẫu (test so sánh tên trường), bên nhận phải đọc được file mẫu.
Đổi hợp đồng = sửa file ở đây trước, rồi sửa code hai bên cho đến khi cả hai test xanh.
