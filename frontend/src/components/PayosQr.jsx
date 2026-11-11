import React, { useEffect, useState } from 'react';
import QRCode from 'qrcode';

/**
 * Vẽ chuỗi VietQR PayOS trả về (payment.qrCode) thành ảnh QR ngay trên web.
 * App ngân hàng nào cũng quét được; PayOS xác nhận chuyển khoản -> webhook/sync -> payment SUCCESS.
 * Không phụ thuộc trang pay.payos.vn (trang đó có lúc bị lỗi 521).
 */
export default function PayosQr({ value, size = 240 }) {
  const [src, setSrc] = useState('');
  const [error, setError] = useState(false);

  useEffect(() => {
    let alive = true;
    setSrc('');
    setError(false);
    QRCode.toDataURL(value, { width: size, margin: 1, errorCorrectionLevel: 'M' })
      .then((url) => alive && setSrc(url))
      .catch(() => alive && setError(true));
    return () => {
      alive = false;
    };
  }, [value, size]);

  if (error) return <p className="text-xs text-red-600">Không tạo được ảnh QR, hãy dùng nút mở trang PayOS.</p>;
  if (!src) return <div className="bg-slate-100 rounded-xl animate-pulse" style={{ width: size, height: size }} />;
  return (
    <div className="flex flex-col items-center gap-2">
      <img src={src} width={size} height={size} alt="Mã QR thanh toán PayOS" className="rounded-xl bg-white p-2 shadow-md" />
      <a href={src} download="payos-qr.png" className="text-[11px] font-bold text-slate-500 hover:text-red-600 underline">
        Tải ảnh QR
      </a>
    </div>
  );
}
