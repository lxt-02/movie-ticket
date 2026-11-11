import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  CreditCard,
  QrCode,
  CheckCircle2,
  Printer,
  Clock,
  ArrowRight,
  AlertCircle,
  RefreshCw,
  ExternalLink,
  XCircle,
} from 'lucide-react';
import {
  getBookingById,
  waitForPayment,
  syncPaymentByBooking,
  pollBookingUntilSettled,
  retryBookingPayment,
  fmtDateTime,
  fmtMoney,
} from '../api';
import PayosQr from '../components/PayosQr';

/**
 * Luồng thật: booking PENDING -> (Kafka) payment-service tạo link PayOS -> khách thanh toán trên PayOS
 * -> webhook / sync -> (Kafka) booking CONFIRMED. Trang này chỉ hiển thị trạng thái thật, không giả lập.
 */
const FAILED_STATUSES = ['CANCELLED', 'EXPIRED', 'FAILED'];

function useCountdown(isoTime) {
  const [left, setLeft] = useState(null);
  useEffect(() => {
    if (!isoTime) return undefined;
    const tick = () => setLeft(Math.max(0, Math.floor((new Date(isoTime) - Date.now()) / 1000)));
    tick();
    const t = setInterval(tick, 1000);
    return () => clearInterval(t);
  }, [isoTime]);
  return left;
}

export default function PaymentPage() {
  const { bookingId } = useParams();

  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [waitingPayment, setWaitingPayment] = useState(false);
  const [checking, setChecking] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [infoMsg, setInfoMsg] = useState('');

  const secondsLeft = useCountdown(booking?.status === 'PENDING' ? booking?.expiresAt : null);

  const loadPayment = useCallback(async () => {
    setWaitingPayment(true);
    try {
      const p = await waitForPayment(bookingId);
      setPayment(p);
      if (!p) setInfoMsg('Chưa nhận được link thanh toán từ payment-service. Bấm "Kiểm tra lại" sau vài giây.');
    } catch (e) {
      setErrorMsg(e.message);
    } finally {
      setWaitingPayment(false);
    }
  }, [bookingId]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    localStorage.setItem('pendingBookingId', bookingId);
    setLoading(true);
    getBookingById(bookingId)
      .then((b) => {
        setBooking(b);
        if (b.status === 'PENDING' || b.status === 'CONFIRMING') loadPayment();
      })
      .catch((e) => setErrorMsg(e.message))
      .finally(() => setLoading(false));
  }, [bookingId, loadPayment]);

  const handleCheckStatus = async () => {
    setChecking(true);
    setErrorMsg('');
    setInfoMsg('');
    try {
      const p = await syncPaymentByBooking(bookingId);
      setPayment(p);
      const b = await pollBookingUntilSettled(bookingId, { timeoutMs: p?.status === 'SUCCESS' ? 15000 : 3000 });
      setBooking(b);
      if (b.status === 'PENDING') setInfoMsg(`PayOS báo trạng thái: ${p?.status || 'chưa thanh toán'}.`);
    } catch (e) {
      setErrorMsg(e.message);
    } finally {
      setChecking(false);
    }
  };

  const handleRetryPayment = async () => {
    setChecking(true);
    setErrorMsg('');
    try {
      const b = await retryBookingPayment(bookingId);
      setBooking(b);
      setPayment(null);
      await loadPayment();
    } catch (e) {
      setErrorMsg(e.message);
    } finally {
      setChecking(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="max-w-xl mx-auto py-20 text-center space-y-3">
        <AlertCircle className="w-10 h-10 text-red-500 mx-auto" />
        <h2 className="text-lg font-bold text-slate-800">Không tải được đơn đặt vé</h2>
        <p className="text-sm text-slate-500">{errorMsg}</p>
        <Link to="/my-tickets" className="text-red-600 font-bold hover:underline text-sm">Về vé của tôi</Link>
      </div>
    );
  }

  const isPaid = booking.status === 'CONFIRMED';
  const isFailed = FAILED_STATUSES.includes(booking.status);
  const amount = booking.totalAmount ?? payment?.amount ?? 0;
  const seatsText = (booking.seats || []).map((s) => s.seatLabel).join(', ');

  return (
    <div className="max-w-4xl mx-auto px-4 lg:px-8 py-10 space-y-8">
      {isPaid ? (
        /* E-TICKET */
        <div className="bg-white border-2 border-emerald-500 rounded-3xl p-6 sm:p-10 shadow-2xl space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-red-600 flex items-center justify-center font-black text-white">MTK</div>
              <div>
                <span className="font-black text-slate-900 uppercase block">VÉ XEM PHIM ĐIỆN TỬ</span>
                <span className="text-xs text-emerald-600 font-bold flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" /> ĐÃ THANH TOÁN THÀNH CÔNG
                </span>
              </div>
            </div>
            <span className="font-mono text-sm font-bold text-slate-500">
              Mã đặt vé: <strong className="text-slate-900">{booking.bookingCode}</strong>
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
            <div className="space-y-3">
              <h2 className="text-2xl font-black text-slate-900 uppercase">{booking.movieTitle}</h2>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <Info label="Rạp & phòng" value={`${booking.cinemaName || ''} • ${booking.screenName || ''}`} />
                <Info label="Suất chiếu" value={fmtDateTime(booking.startTime)} />
                <Info label="Ghế" value={seatsText} highlight />
                <Info label="Tổng tiền" value={fmtMoney(amount)} />
              </div>
            </div>
            <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 space-y-2">
              <div className="text-xs font-bold text-slate-600 uppercase">Mã vé từng ghế</div>
              {(booking.seats || []).map((s) => (
                <div key={s.id} className="flex justify-between text-sm font-mono">
                  <span className="font-bold text-red-600">{s.seatLabel}</span>
                  <span className="text-slate-800">{s.ticketCode || '—'}</span>
                </div>
              ))}
              <p className="text-[10px] text-slate-500 pt-2">Xuất trình mã đặt vé hoặc mã vé tại quầy soát vé</p>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 flex flex-wrap items-center justify-between gap-4">
            <button onClick={() => window.print()} className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs flex items-center gap-2">
              <Printer className="w-4 h-4" /> In vé
            </button>
            <Link to="/my-tickets" className="px-6 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase flex items-center gap-2">
              Xem trong vé của tôi <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      ) : isFailed ? (
        <div className="bg-white border border-red-200 rounded-3xl p-8 shadow-xl text-center space-y-4">
          <XCircle className="w-12 h-12 text-red-500 mx-auto" />
          <h1 className="text-xl font-black text-slate-900 uppercase">Đơn đặt vé {booking.status === 'EXPIRED' ? 'đã hết hạn' : 'đã bị huỷ'}</h1>
          <p className="text-sm text-slate-500">Ghế đã được trả lại. Bạn có thể chọn suất chiếu và đặt lại.</p>
          <Link to="/movies" className="inline-block px-6 py-3 rounded-2xl bg-red-600 text-white font-black text-xs uppercase">Đặt vé mới</Link>
        </div>
      ) : (
        /* PAYMENT */
        <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-xl space-y-8">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-50 text-red-600 inline-block mb-2">
                CỔNG THANH TOÁN PAYOS
              </span>
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 uppercase tracking-tight">Thanh toán đơn đặt vé</h1>
              <p className="text-xs text-slate-500 mt-1">Quét mã VietQR bằng app ngân hàng, chuyển khoản xong bấm "Tôi đã thanh toán – kiểm tra lại".</p>
            </div>
            {secondsLeft != null && (
              <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold">
                <Clock className="w-4 h-4 text-amber-600" />
                Giữ ghế còn:
                <span className="font-mono text-sm text-red-600">
                  {String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:{String(secondsLeft % 60).padStart(2, '0')}
                </span>
              </div>
            )}
          </div>

          {errorMsg && (
            <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" /> {errorMsg}
            </div>
          )}
          {infoMsg && (
            <div className="p-4 rounded-2xl bg-blue-50 border border-blue-200 text-blue-700 text-xs">{infoMsg}</div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
            <div className="flex flex-col items-center justify-center p-6 bg-slate-50 rounded-3xl border-2 border-dashed border-slate-300 text-center space-y-4">
              {payment?.qrCode ? (
                <>
                  <PayosQr value={payment.qrCode} />
                  <p className="text-xs text-slate-600">Mở app ngân hàng và quét mã VietQR này để thanh toán</p>
                </>
              ) : (
                <QrCode className="w-24 h-24 text-slate-300" />
              )}
              {waitingPayment ? (
                <p className="text-xs text-slate-500 flex items-center gap-2">
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" /> Đang chờ payment-service tạo link PayOS...
                </p>
              ) : payment?.checkoutUrl ? (
                <a
                  href={payment.checkoutUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-5 py-2.5 rounded-2xl border border-slate-300 hover:bg-white text-slate-700 font-bold text-xs uppercase flex items-center gap-2"
                >
                  <ExternalLink className="w-4 h-4" /> Hoặc mở trang PayOS
                </a>
              ) : (
                <p className="text-xs text-slate-500">Chưa có link thanh toán.</p>
              )}
              {payment && (
                <p className="text-[11px] text-slate-500 font-mono">
                  {payment.paymentCode} • trạng thái: <strong>{payment.status}</strong>
                </p>
              )}
            </div>

            <div className="space-y-5">
              <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-3 text-xs">
                <Row label="Mã đơn" value={booking.bookingCode} mono />
                <Row label="Phim" value={booking.movieTitle} />
                <Row label="Suất chiếu" value={fmtDateTime(booking.startTime)} />
                <Row label="Ghế" value={seatsText} />
                <Row label="Trạng thái" value={booking.status} />
                <div className="flex justify-between pt-2 border-t border-slate-200">
                  <span className="text-slate-700 font-bold">Tổng thanh toán:</span>
                  <span className="font-black text-red-600 text-sm font-mono">{fmtMoney(amount)}</span>
                </div>
              </div>

              <button
                onClick={handleCheckStatus}
                disabled={checking}
                className="w-full py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-black text-sm uppercase flex items-center justify-center gap-2"
              >
                <RefreshCw className={`w-5 h-5 ${checking ? 'animate-spin' : ''}`} />
                {checking ? 'Đang kiểm tra với PayOS...' : 'Tôi đã thanh toán – kiểm tra lại'}
              </button>
              {payment && ['FAILED', 'EXPIRED', 'CANCELLED'].includes(payment.status) && (
                <button
                  onClick={handleRetryPayment}
                  disabled={checking}
                  className="w-full py-3 rounded-2xl border border-slate-300 text-slate-700 font-bold text-xs uppercase flex items-center justify-center gap-2"
                >
                  <CreditCard className="w-4 h-4" /> Tạo lại link thanh toán
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Info({ label, value, highlight }) {
  return (
    <div>
      <span className="text-slate-400 block mb-0.5">{label}:</span>
      <span className={`font-bold ${highlight ? 'text-red-600 text-sm' : 'text-slate-800'}`}>{value}</span>
    </div>
  );
}

function Row({ label, value, mono }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-slate-500">{label}:</span>
      <span className={`font-bold text-slate-900 text-right ${mono ? 'font-mono' : ''}`}>{value}</span>
    </div>
  );
}
