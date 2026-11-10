import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

/**
 * Renders the VietQR string returned by PayOS (payment.qrCode) as an image.
 * Any banking app can scan it; PayOS confirms the transfer and the payment becomes SUCCESS.
 */
export function PayosQr({ value, size = 260 }: { value: string; size?: number }) {
  const [src, setSrc] = useState<string>();
  const [error, setError] = useState(false);

  useEffect(() => {
    let alive = true;
    setError(false);
    QRCode.toDataURL(value, { width: size, margin: 1, errorCorrectionLevel: 'M' })
      .then((url) => alive && setSrc(url))
      .catch(() => alive && setError(true));
    return () => {
      alive = false;
    };
  }, [value, size]);

  if (error) return <p className="warn-text">Không tạo được ảnh QR. Vui lòng dùng nút "Mở trang PayOS".</p>;
  if (!src) return <div className="qr-box" style={{ width: size, height: size }} />;
  return (
    <div className="qr-box">
      <img src={src} width={size} height={size} alt="Mã QR thanh toán PayOS" />
      <a className="small" href={src} download="payos-qr.png">Tải ảnh QR</a>
    </div>
  );
}
