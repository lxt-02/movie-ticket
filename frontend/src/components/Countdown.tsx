import { useEffect, useRef, useState } from 'react';

/** Countdown to booking.expiresAt (~7 minutes after creating the booking, FRONTEND_API §3.3) */
export function Countdown({ to, onExpire }: { to: string; onExpire?: () => void }) {
  const end = new Date(to).getTime();
  const [left, setLeft] = useState(Math.max(0, end - Date.now()));
  const fired = useRef(false);

  useEffect(() => {
    const t = setInterval(() => {
      const l = Math.max(0, end - Date.now());
      setLeft(l);
      if (l === 0 && !fired.current) {
        fired.current = true;
        // booking expiry job runs every 30s (+90s grace) – reload a bit later
        setTimeout(() => onExpire?.(), 5000);
      }
    }, 1000);
    return () => clearInterval(t);
  }, [end, onExpire]);

  const m = Math.floor(left / 60000);
  const s = Math.floor((left % 60000) / 1000);
  return (
    <div className={`countdown ${left < 60000 ? 'urgent' : ''}`}>
      {left > 0 ? <>Giữ ghế còn <strong>{m}:{String(s).padStart(2, '0')}</strong></> : 'Đã hết thời gian thanh toán'}
    </div>
  );
}
