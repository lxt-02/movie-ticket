import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { bookingsApi, paymentsApi } from '../api/endpoints';
import type { Payment } from '../api/types';
import { ErrorBox, Loading, useAction } from '../components/ui';
import { TicketList } from '../components/TicketList';
import { pendingBooking, pollBookingUntilSettled, type Outcome } from '../utils/paymentFlow';

/**
 * PayOS returns here: /payment/success or /payment/cancel. URL params are NOT trusted (FRONTEND_API §3.5):
 * take the saved bookingId → POST /payments/by-booking/{id}/sync → poll booking until settled.
 */
export default function PaymentReturnPage({ kind }: { kind: 'success' | 'cancel' }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  // Read once on mount: pendingBooking is cleared after settling, re-reading it each render would lose the id
  const [bookingId] = useState(() => pendingBooking.get() ?? params.get('bookingId'));
  const [outcome, setOutcome] = useState<Outcome>();
  const [error, setError] = useState<unknown>();
  const [round, setRound] = useState(0);
  const action = useAction();

  useEffect(() => {
    if (!bookingId) return;
    const ctrl = new AbortController();
    (async () => {
      setError(undefined);
      let payment: Payment | undefined;
      try {
        payment = await paymentsApi.syncByBooking(bookingId);
      } catch (e) {
        setError(e); // still look at the booking
      }
      try {
        const final = await pollBookingUntilSettled(bookingId, payment, setOutcome, ctrl.signal);
        if (final.kind === 'confirmed' || final.kind === 'failed') pendingBooking.clear();
      } catch (e) {
        setError(e);
      }
    })();
    return () => ctrl.abort();
  }, [bookingId, round]);

  if (!bookingId) {
    return (
      <div className="card">
        <h2>Không xác định được đơn đặt vé</h2>
        <p>Vui lòng mở <Link to="/my-bookings">Vé của tôi</Link> để kiểm tra trạng thái.</p>
      </div>
    );
  }

  const retry = () =>
    action.run(async () => {
      await bookingsApi.retryPayment(bookingId);
      navigate(`/checkout/${bookingId}`);
    });

  return (
    <div className="card result">
      {!outcome && <Loading text={kind === 'success' ? 'Đang kiểm tra thanh toán với PayOS…' : 'Đang kiểm tra trạng thái đơn…'} />}

      {outcome?.kind === 'confirming' && (
        <>
          <h2>⏳ Đã nhận thanh toán, đang xác nhận ghế…</h2>
          <p className="muted">Việc này có thể mất vài giây đến hơn 10 giây.</p>
          <button className="ghost" onClick={() => setRound((r) => r + 1)}>Kiểm tra lại</button>
        </>
      )}

      {outcome?.kind === 'confirmed' && (
        <>
          <h2 className="ok">✅ Đặt vé thành công</h2>
          <p>Mã đặt vé: <strong>{outcome.booking.bookingCode}</strong></p>
          <TicketList booking={outcome.booking} />
          <Link to={`/my-bookings/${outcome.booking.id}`}>Xem chi tiết</Link>
        </>
      )}

      {outcome?.kind === 'unpaid' && (
        <>
          <h2>Chưa thanh toán</h2>
          <p>{kind === 'cancel' ? 'Bạn đã huỷ trên trang PayOS.' : 'Hệ thống chưa nhận được tiền cho đơn này.'} Đơn vẫn đang giữ ghế cho tới khi hết hạn.</p>
          <div className="row actions">
            <button className="primary" disabled={action.busy} onClick={retry}>Thanh toán lại</button>
            <button className="ghost" onClick={() => setRound((r) => r + 1)}>Tôi đã chuyển khoản – kiểm tra lại</button>
            <Link to={`/checkout/${bookingId}`}>Về trang thanh toán</Link>
          </div>
        </>
      )}

      {outcome?.kind === 'failed' && (
        <>
          <h2 className="bad">❌ Đơn {outcome.booking.status === 'EXPIRED' ? 'đã hết hạn' : 'đã bị huỷ'}</h2>
          {outcome.paidButFailed ? (
            <p className="warn-text">Bạn đã thanh toán nhưng không giữ được ghế. Vui lòng liên hệ quầy vé với mã đơn{' '}
              <strong>{outcome.booking.bookingCode}</strong> để được hỗ trợ (hệ thống không hoàn tiền tự động).</p>
          ) : <p>Ghế đã được nhả. Bạn có thể đặt lại.</p>}
          <Link to="/">Về trang chủ</Link>
        </>
      )}

      <ErrorBox error={error ?? action.error} />
    </div>
  );
}
