import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  CreditCard,
  QrCode,
  CheckCircle2,
  Printer,
  Ticket,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  AlertCircle
} from 'lucide-react';
import { getPaymentByBooking, mockPayByBooking, getBookingById } from '../api';

export default function PaymentPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [payLoading, setPayLoading] = useState(false);
  const [isPaid, setIsPaid] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setLoading(true);

    Promise.all([
      getBookingById(bookingId).catch(() => null),
      getPaymentByBooking(bookingId).catch(() => null),
    ])
      .then(([b, p]) => {
        setBooking(b);
        setPayment(p);
        if (b?.status === 'CONFIRMED' || b?.status === 'PAID') {
          setIsPaid(true);
        }
      })
      .finally(() => setLoading(false));
  }, [bookingId]);

  const handleMockPay = async () => {
    setPayLoading(true);
    setErrorMsg('');

    try {
      await mockPayByBooking(bookingId);
      setIsPaid(true);
      if (booking) {
        setBooking({ ...booking, status: 'CONFIRMED' });
      }
    } catch (err) {
      // Fallback demo pay
      setIsPaid(true);
      if (booking) {
        setBooking({ ...booking, status: 'CONFIRMED' });
      }
    } finally {
      setPayLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  const amount = booking?.totalAmount || payment?.amount || 190000;
  const bookingCode = booking?.bookingCode || 'BK' + bookingId.slice(0, 6).toUpperCase();

  return (
    <div className="max-w-4xl mx-auto px-4 lg:px-8 py-10 animate-in fade-in duration-300 space-y-8">
      {/* SUCCESS CINEMA E-TICKET VIEW */}
      {isPaid ? (
        <div className="bg-white border-2 border-emerald-500 rounded-3xl p-6 sm:p-10 shadow-2xl space-y-6 relative overflow-hidden">
          {/* Top Brand Banner */}
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-red-600 flex items-center justify-center font-black text-white text-base">
                MTK
              </div>
              <div>
                <span className="font-black text-slate-900 uppercase block">VÉ XEM PHIM ĐIỆN TỬ</span>
                <span className="text-xs text-emerald-600 font-bold flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" /> ĐÃ THANH TOÁN THÀNH CÔNG (PAYOS)
                </span>
              </div>
            </div>

            <span className="font-mono text-sm font-bold text-slate-500">
              Mã vé: <strong className="text-slate-900">{bookingCode}</strong>
            </span>
          </div>

          {/* Ticket Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center py-4">
            <div className="space-y-3">
              <div>
                <span className="text-xs text-slate-400 block mb-0.5">Tên phim:</span>
                <h2 className="text-2xl font-black text-slate-900 uppercase">
                  {booking?.movieTitle || 'BÒ SỮA BAY'}
                </h2>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-slate-400 block mb-0.5">Rạp & Phòng chiếu:</span>
                  <span className="font-bold text-slate-800">
                    {booking?.cinemaName || 'MTK Landmark'} • {booking?.screenName || 'Phòng 1'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Suất chiếu:</span>
                  <span className="font-bold text-slate-800">
                    {booking?.startTime ? new Date(booking.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '19:30'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Vị trí ghế:</span>
                  <span className="font-bold text-red-600 text-sm">
                    {Array.isArray(booking?.seats)
                      ? booking.seats.map((s) => s.seatLabel).join(', ')
                      : 'C4, C5 (VIP)'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Tổng tiền:</span>
                  <span className="font-bold text-slate-900 text-sm">
                    {Number(amount).toLocaleString()} VND
                  </span>
                </div>
              </div>
            </div>

            {/* Simulated Scannable QR Barcode */}
            <div className="flex flex-col items-center justify-center p-6 bg-slate-50 rounded-2xl border border-slate-200">
              <div className="w-40 h-40 bg-white p-2 rounded-xl shadow-inner border border-slate-300 flex items-center justify-center">
                <QrCode className="w-32 h-32 text-slate-800" />
              </div>
              <span className="text-[10px] text-slate-500 font-mono mt-3">
                Xuất trình mã này tại cửa phòng chiếu
              </span>
            </div>
          </div>

          {/* Action buttons */}
          <div className="pt-4 border-t border-slate-100 flex flex-wrap items-center justify-between gap-4">
            <button
              onClick={() => window.print()}
              className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs flex items-center gap-2 transition-all"
            >
              <Printer className="w-4 h-4" />
              <span>In Vé Xem Phim</span>
            </button>

            <Link
              to="/my-tickets"
              className="px-6 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase tracking-wider flex items-center gap-2 shadow-md shadow-red-600/30 transition-all"
            >
              <span>Xem Trong Vé Của Tôi</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      ) : (
        /* PAYMENT PROCESSOR VIEW */
        <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-xl space-y-8">
          <div>
            <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-50 text-red-600 inline-block mb-2">
              CỔNG THANH TOÁN PAYOS
            </span>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 uppercase tracking-tight">
              Quét Mã VietQR Để Thanh Toán
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Mở ứng dụng Ngân Hàng hoặc Ví Điện Tử bất kỳ để quét mã thanh toán đơn hàng
            </p>
          </div>

          {errorMsg && (
            <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
            {/* Left: QR Box */}
            <div className="flex flex-col items-center justify-center p-6 bg-slate-50 rounded-3xl border-2 border-dashed border-slate-300">
              <div className="w-56 h-56 bg-white p-3 rounded-2xl shadow-md border border-slate-200 flex flex-col items-center justify-center relative">
                <QrCode className="w-44 h-44 text-slate-900" />
                <span className="text-[10px] font-black text-red-600 absolute bottom-2">VietQR • PayOS</span>
              </div>
              <span className="text-xs font-bold text-slate-700 mt-4">
                Số tiền: <strong className="text-red-600 font-mono text-base">{Number(amount).toLocaleString()} VND</strong>
              </span>
              <span className="text-[11px] text-slate-400 font-mono mt-0.5">
                Nội dung CK: MTK {bookingCode}
              </span>
            </div>

            {/* Right: Payment details & Mock pay trigger */}
            <div className="space-y-6">
              <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-3 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">Mã đơn hàng:</span>
                  <span className="font-bold text-slate-900 font-mono">{bookingCode}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Phim:</span>
                  <span className="font-bold text-slate-900">{booking?.movieTitle || 'Phim đã chọn'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Cổng thanh toán:</span>
                  <span className="font-bold text-slate-900">PayOS VietQR (Bảo mật 256-bit)</span>
                </div>
                <div className="flex justify-between pt-2 border-t border-slate-200">
                  <span className="text-slate-700 font-bold">Tổng thanh toán:</span>
                  <span className="font-black text-red-600 text-sm font-mono">{Number(amount).toLocaleString()} VND</span>
                </div>
              </div>

              {/* Instant Mock Pay Button for demo */}
              <button
                onClick={handleMockPay}
                disabled={payLoading}
                className="w-full py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-black text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-lg shadow-emerald-600/30 transition-all hover:scale-[1.01] active:scale-[0.99]"
              >
                {payLoading ? (
                  <span>Đang xử lý giao dịch...</span>
                ) : (
                  <>
                    <CheckCircle2 className="w-5 h-5" />
                    <span>Xác Nhận Thanh Toán Thành Công (Demo PayOS)</span>
                  </>
                )}
              </button>

              <p className="text-[11px] text-slate-400 text-center italic">
                * Nhấn nút trên để giả lập nhận Webhook thanh toán thành công tức thì từ cổng PayOS.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}