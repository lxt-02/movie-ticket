import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, RefreshCw } from 'lucide-react';
import { syncPaymentByBooking, pollBookingUntilSettled } from '../api';

/**
 * PayOS redirect về /payment/success hoặc /payment/cancel. Không tin query string PayOS gắn vào URL:
 * lấy bookingId đã lưu -> POST /payments/by-booking/{id}/sync -> chờ booking ra trạng thái cuối.
 */
export default function PaymentReturnPage({ kind }) {
  const navigate = useNavigate();
  const [state, setState] = useState({ loading: true });

  useEffect(() => {
    const bookingId = localStorage.getItem('pendingBookingId');
    if (!bookingId) {
      setState({ loading: false, error: 'Không tìm thấy đơn đặt vé đang thanh toán.' });
      return;
    }
    (async () => {
      try {
        const payment = await syncPaymentByBooking(bookingId);
        const booking = await pollBookingUntilSettled(bookingId, {
          timeoutMs: payment?.status === 'SUCCESS' ? 20000 : 3000,
        });
        if (booking.status === 'CONFIRMED') {
          localStorage.removeItem('pendingBookingId');
          navigate(`/payment/${bookingId}`, { replace: true });
          return;
        }
        setState({ loading: false, bookingId, booking, payment });
      } catch (e) {
        setState({ loading: false, bookingId, error: e.message });
      }
    })();
  }, [navigate]);

  if (state.loading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-3 text-sm text-slate-600">
        <RefreshCw className="w-8 h-8 text-red-600 animate-spin" />
        Đang xác nhận kết quả thanh toán với PayOS...
      </div>
    );
  }

  return (
    <div className="max-w-xl mx-auto py-20 px-4 text-center space-y-4">
      {kind === 'success' && !state.error ? (
        <CheckCircle2 className="w-12 h-12 text-amber-500 mx-auto" />
      ) : (
        <XCircle className="w-12 h-12 text-red-500 mx-auto" />
      )}
      <h1 className="text-xl font-black text-slate-900 uppercase">
        {kind === 'cancel' ? 'Bạn đã huỷ thanh toán' : 'Chưa xác nhận được thanh toán'}
      </h1>
      <p className="text-sm text-slate-500">
        {state.error ||
          `Trạng thái payment: ${state.payment?.status || '—'}, booking: ${state.booking?.status || '—'}.`}
      </p>
      {state.bookingId && (
        <Link to={`/payment/${state.bookingId}`} className="inline-block px-6 py-3 rounded-2xl bg-red-600 text-white font-black text-xs uppercase">
          Quay lại trang thanh toán
        </Link>
      )}
    </div>
  );
}
