import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { bookingsApi, paymentsApi } from '../api/endpoints';
import type { Payment } from '../api/types';
import { ErrorBox, Loading, StatusBadge, useAction, useAsync } from '../components/ui';
import { Countdown } from '../components/Countdown';
import { PayosQr } from '../components/PayosQr';
import { fmtDateTime, fmtMoney } from '../utils/format';
import { pendingBooking, waitForPaymentLink } from '../utils/paymentFlow';

/** How often the page asks payment-service to check PayOS while the QR is shown. */
const SCAN_POLL_MS = 3000;

/**
 * After POST /bookings: wait for the PayOS link, show the PayOS QR (payment.qrCode) and countdown.
 * While the QR is shown the page polls POST /payments/by-booking/{id}/sync, so a successful scan
 * moves to the success page even when the PayOS webhook cannot reach a local backend.
 * Also: retry payment (max 3 links), cancel booking, "I have paid" (sync). FRONTEND_API §3.3–3.7
 */
export default function CheckoutPage() {
  const { bookingId = '' } = useParams();
  const navigate = useNavigate();
  const booking = useAsync(() => bookingsApi.get(bookingId), [bookingId]);
  const [payment, setPayment] = useState<Payment>();
  const [waiting, setWaiting] = useState(false);
  const [linkTimeout, setLinkTimeout] = useState(false);
  const action = useAction();
  const abortRef = useRef<AbortController>();

  const b = booking.data;

  const fetchLink = useCallback(async (previousId?: string) => {
    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;
    setWaiting(true);
    setLinkTimeout(false);
    try {
      const p = await waitForPaymentLink(bookingId, { previousId, signal: ctrl.signal });
      if (ctrl.signal.aborted) return;
      if (p) setPayment(p);
      else setLinkTimeout(true);
      if (p?.status === 'SUCCESS') navigate('/payment/success');
    } catch (e) {
      action.setError(e);
    } finally {
      if (!ctrl.signal.aborted) setWaiting(false);
    }
  }, [bookingId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (b?.status === 'PENDING' && !payment && !waiting) fetchLink();
    return undefined;
  }, [b?.status]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => () => abortRef.current?.abort(), []);

  // Wait for the customer to scan: poll payment status until PayOS reports the transfer.
  const scanning = b?.status === 'PENDING' && (payment?.status === 'PENDING' || payment?.status === 'PROCESSING');
  useEffect(() => {
    if (!scanning) return undefined;
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    const tick = async () => {
      if (stopped) return;
      if (!document.hidden) {
        try {
          const p = await paymentsApi.syncByBooking(bookingId);
          if (stopped) return;
          if (p.status === 'SUCCESS') {
            pendingBooking.save(bookingId);
            navigate('/payment/success');
            return;
          }
          if (p.status === 'FAILED' || p.status === 'EXPIRED' || p.status === 'CANCELLED') {
            setPayment(p);
            booking.reload();
            return;
          }
        } catch {
          // transient error: keep polling
        }
      }
      timer = setTimeout(tick, SCAN_POLL_MS);
    };
    timer = setTimeout(tick, SCAN_POLL_MS);
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [scanning, payment?.id, bookingId]); // eslint-disable-line react-hooks/exhaustive-deps

  const goToPayOS = () => {
    if (!payment?.checkoutUrl) return;
    pendingBooking.save(bookingId); // PayOS return URL has no bookingId
    window.location.href = payment.checkoutUrl;
  };

  const retry = () =>
    action.run(async () => {
      const prevId = payment?.id;
      await bookingsApi.retryPayment(bookingId); // 202
      setPayment(undefined);
      await fetchLink(prevId);
    });

  const cancel = () =>
    action.run(async () => {
      if (!confirm('Huỷ đơn đặt vé này? Ghế sẽ được nhả ngay.')) return;
      await bookingsApi.cancel(bookingId, 'Khách huỷ trên web');
      abortRef.current?.abort();
      await booking.reload();
    });

  const iPaid = () =>
    action.run(async () => {
      await paymentsApi.syncByBooking(bookingId);
      pendingBooking.save(bookingId);
      navigate('/payment/success');
    });

  if (booking.loading && !b) return <Loading />;
  if (!b) return <ErrorBox error={booking.error} />;

  return (
    <div className="checkout">
      <div className="card">
        <h2>Thanh toán đơn {b.bookingCode} <StatusBadge value={b.status} /></h2>
        <div>{b.movieTitle} · {b.cinemaName} · {b.screenName}</div>
        <div>Suất chiếu: <strong>{fmtDateTime(b.startTime)}</strong></div>
        <div>Ghế: {b.seats.map((s) => s.seatLabel).join(', ')}</div>
        <div className="total">Tổng tiền: <strong>{fmtMoney(b.totalAmount, b.currency)}</strong>
          {b.discountAmount > 0 && <span className="muted small"> (giảm {fmtMoney(b.discountAmount)})</span>}
        </div>
      </div>

      {b.status === 'PENDING' ? (
        <div className="card">
          <Countdown to={b.expiresAt} onExpire={() => booking.reload()} />
          {waiting && <Loading text="Đang tạo link thanh toán PayOS…" />}
          {linkTimeout && <p className="warn-text">Chưa tạo được link thanh toán. Bấm "Thanh toán lại" để thử lần nữa.</p>}
          {payment?.status === 'PENDING' && payment.qrCode && (
            <div className="qr-pay">
              <strong>Quét mã QR bằng ứng dụng ngân hàng để thanh toán</strong>
              <PayosQr value={payment.qrCode} />
              <div className="amount">{fmtMoney(payment.amount, payment.currency)}</div>
              <div className="muted small">Mã thanh toán: {payment.paymentCode}</div>
              <Loading text="Đang chờ thanh toán… trang sẽ tự chuyển khi nhận được tiền" />
            </div>
          )}
          {payment?.status === 'PENDING' && payment.checkoutUrl && (
            <p className="row">
              <button className={payment.qrCode ? 'ghost' : 'primary'} onClick={goToPayOS}>
                {payment.qrCode ? 'Mở trang PayOS' : 'Thanh toán qua PayOS →'}
              </button>
            </p>
          )}
          {payment && (payment.status === 'FAILED' || payment.status === 'EXPIRED' || payment.status === 'CANCELLED') && (
            <p className="warn-text">Link thanh toán đã {payment.status === 'EXPIRED' ? 'hết hạn' : 'không còn dùng được'}. Bấm "Thanh toán lại" để tạo mã QR mới.</p>
          )}
          <div className="row actions">
            <button className="ghost" disabled={action.busy} onClick={iPaid}>Tôi đã chuyển khoản</button>
            <button className="ghost" disabled={action.busy || waiting} onClick={retry}>Thanh toán lại</button>
            <button className="danger" disabled={action.busy} onClick={cancel}>Huỷ đơn</button>
          </div>
          <p className="muted small">Mỗi đơn được tạo tối đa 3 link thanh toán. Đơn chỉ huỷ được khi chưa thanh toán.</p>
          <ErrorBox error={action.error} />
        </div>
      ) : (
        <div className="card">
          <p>Đơn đang ở trạng thái <StatusBadge value={b.status} />.</p>
          <Link to={`/my-bookings/${b.id}`}>Xem chi tiết vé</Link>
        </div>
      )}
    </div>
  );
}
