// Kiểm tra bộ key PayOS trong services/payment-service/.env
// Chạy:  node services/payment-service/scripts/payos-check.mjs
// Tạo 1 link test 2.000đ (ký giống hệt PayosSignature.java) rồi huỷ ngay.
import { readFileSync } from 'node:fs';
import { createHmac } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const envPath = join(dirname(fileURLToPath(import.meta.url)), '..', '.env');
const env = Object.fromEntries(
  readFileSync(envPath, 'utf8').split(/\r?\n/)
    .filter((l) => l && !l.startsWith('#') && l.includes('='))
    .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()]),
);
const { PAYOS_CLIENT_ID: clientId, PAYOS_API_KEY: apiKey, PAYOS_CHECKSUM_KEY: checksumKey } = env;
const returnUrl = env.PAYOS_RETURN_URL || 'http://localhost:3000/payment/success';
const cancelUrl = env.PAYOS_CANCEL_URL || 'http://localhost:3000/payment/cancel';
const base = env.PAYOS_BASE_URL || 'https://api-merchant.payos.vn';
if (!clientId || !apiKey || !checksumKey) throw new Error('Thiếu PAYOS_CLIENT_ID / PAYOS_API_KEY / PAYOS_CHECKSUM_KEY trong ' + envPath);

const orderCode = Number(String(Date.now()).slice(-10));
const amount = 2000;
const description = 'TEST' + String(orderCode).slice(-5);
const data = `amount=${amount}&cancelUrl=${cancelUrl}&description=${description}&orderCode=${orderCode}&returnUrl=${returnUrl}`;
const signature = createHmac('sha256', checksumKey).update(data).digest('hex');
const headers = { 'x-client-id': clientId, 'x-api-key': apiKey, 'Content-Type': 'application/json' };

const res = await fetch(`${base}/v2/payment-requests`, {
  method: 'POST', headers,
  body: JSON.stringify({ orderCode, amount, description, cancelUrl, returnUrl, signature }),
});
const json = await res.json();
console.log('PayOS trả:', json.code, '-', json.desc);
if (json.code === '00') {
  console.log('OK: key hợp lệ. qrCode =', json.data?.qrCode?.slice(0, 40) + '…');
  await fetch(`${base}/v2/payment-requests/${orderCode}/cancel`, {
    method: 'POST', headers, body: JSON.stringify({ cancellationReason: 'payos-check' }),
  });
  console.log('Đã huỷ link test', orderCode);
} else if (json.code === '201') {
  console.log('=> Checksum Key KHÔNG khớp với Client ID. Copy lại cả 3 key từ CÙNG một kênh thanh toán trên my.payos.vn.');
} else {
  console.log('=> Lỗi khác, xem desc ở trên (code 214/401 thường là sai Client ID / API Key).');
}
