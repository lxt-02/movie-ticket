import React, { useState, useEffect } from 'react';
import { X, CheckCircle, QrCode, CreditCard, Sparkles, AlertCircle, RefreshCw, Ticket } from 'lucide-react';
import { mockPayByBooking, getPaymentByBooking } from '../api';

export default function PaymentModal({ booking, selectedSeats, showtime, movie, onClose, onComplete }) {
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const fetchPayment = async () => {
    if (!booking) return;
    try {
      const p = await getPaymentByBooking(booking.id);
      setPayment(p);
      if (p.status === 'SUCCESS') {
        setIsSuccess(true);
      }
    } catch (e) {
      // Payment might still be creating asynchronously via Kafka outbox
      console.log('Polling payment...', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayment();
    // Poll every 3 seconds until payment is created or succeeded
    const interval = setInterval(fetchPayment, 3000);
    return () => clearInterval(interval);
  }, [booking]);

  const handleMockPay = async () => {
    setPaying(true);
    setErrorMsg('');
    try {
      const res = await mockPayByBooking(booking.id);
      setPayment(res);
      setIsSuccess(true);
      if (onComplete) onComplete(res);
    } catch (err) {
      setErrorMsg(err.message || 'Thanh toán giả lập thất bại');
    } finally {
      setPaying(false);
    }
  };

  if (!booking) return null;

  const totalAmount = booking.finalAmount || booking.totalAmount || selectedSeats.reduce((acc, s) => acc + (s.price || 95000), 0);
  const qrUrl = payment?.qrCode?.startsWith('http')
    ? payment.qrCode
    : `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=CINETICKET_${booking.bookingCode || booking.id}`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col">
        {/* Top Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/50">
          <div className="flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-rose-400" />
            <h3 className="text-base font-bold text-white">
              {isSuccess ? 'Vé Điện Tử Của Bạn' : 'Thanh Toán Đơn Vé'}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6">
          {isSuccess ? (
            /* --- SUCCESS TICKET VIEW --- */
            <div className="space-y-6 animate-in zoom-in-95 duration-300">
              <div className="text-center">
                <div className="w-14 h-14 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-3">
                  <CheckCircle className="w-8 h-8" />
                </div>
                <h4 className="text-xl font-extrabold text-white">Thanh Toán Thành Công!</h4>
                <p className="text-xs text-slate-400 mt-1">Đơn vé đã được xác nhận vào hệ thống Microservices.</p>
              </div>

              {/* Digital Cinema Ticket Card */}
              <div className="bg-gradient-to-b from-slate-950 to-slate-900 border border-emerald-500/40 rounded-2xl p-5 shadow-xl relative overflow-hidden">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <span className="text-[10px] uppercase font-bold tracking-widest text-emerald-400 block mb-1">
                      Vé Vào Cổng Điện Tử
                    </span>
                    <h5 className="text-lg font-black text-white">{movie.title}</h5>
                  </div>
                  <span className="px-2 py-1 rounded bg-emerald-950 text-emerald-300 border border-emerald-700 text-[10px] font-bold">
                    ĐÃ XÁC NHẬN
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs border-t border-b border-slate-800 py-3 mb-4">
                  <div>
                    <span className="text-slate-500 block">Rạp & Phòng:</span>
                    <span className="font-semibold text-slate-200">
                      {showtime.cinemaName} • {showtime.screenName}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Suất chiếu:</span>
                    <span className="font-bold text-rose-400 text-sm">
                      {showtime.startTime} ({showtime.date || 'Hôm nay'})
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Ghế ngồi:</span>
                    <span className="font-bold text-emerald-400 text-sm">
                      {selectedSeats.map((s) => s.seatCode).join(', ')}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Mã đơn vé:</span>
                    <span className="font-mono text-slate-300 text-xs">
                      {booking.bookingCode || booking.id.substring(0, 12)}
                    </span>
                  </div>
                </div>

                {/* QR Code For Scanner */}
                <div className="flex flex-col items-center justify-center p-3 bg-white rounded-xl">
                  <img
                    src={`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=TICKET_OK_${booking.bookingCode || booking.id}`}
                    alt="Ticket QR"
                    className="w-36 h-36"
                  />
                  <span className="text-[10px] font-mono text-slate-700 mt-1">Đưa mã này cho nhân viên soát vé</span>
                </div>
              </div>

              <button
                onClick={onClose}
                className="w-full py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-sm transition-colors"
              >
                Đóng & Quay Về Trang Chủ
              </button>
            </div>
          ) : (
            /* --- PAYMENT PENDING VIEW --- */
            <div className="space-y-6">
              {/* Order Info Summary */}
              <div className="bg-slate-950/70 p-4 rounded-2xl border border-slate-800 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-400">Phim:</span>
                  <span className="font-bold text-white text-right">{movie.title}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Suất chiếu:</span>
                  <span className="font-medium text-slate-200">
                    {showtime.startTime} • {showtime.cinemaName}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Ghế chọn:</span>
                  <span className="font-bold text-rose-400">
                    {selectedSeats.map((s) => s.seatCode).join(', ')}
                  </span>
                </div>
                <div className="flex justify-between pt-2 border-t border-slate-800/80 text-sm">
                  <span className="font-bold text-slate-300">Tổng thanh toán:</span>
                  <span className="font-black text-rose-400 text-base">
                    {totalAmount.toLocaleString('vi-VN')} đ
                  </span>
                </div>
              </div>

              {/* PayOS QR Display */}
              <div className="flex flex-col items-center justify-center p-4 bg-slate-950 rounded-2xl border border-slate-800">
                <span className="text-xs font-semibold text-slate-400 mb-3 flex items-center gap-1.5">
                  <QrCode className="w-4 h-4 text-rose-400" />
                  Quét mã QR PayOS để thanh toán
                </span>
                <div className="p-3 bg-white rounded-xl shadow-lg mb-2">
                  <img src={qrUrl} alt="PayOS QR" className="w-44 h-44 object-contain" />
                </div>
                <span className="text-[11px] text-slate-500 font-mono">
                  Mã đơn: {booking.bookingCode || booking.id.substring(0, 18)}
                </span>
              </div>

              {errorMsg && (
                <div className="p-3 rounded-xl bg-rose-950/50 border border-rose-800/80 text-rose-300 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
                  <span>{errorMsg}</span>
                </div>
              )}

              {/* Action Buttons */}
              <div className="space-y-3">
                <button
                  disabled={paying}
                  onClick={handleMockPay}
                  className="w-full py-3.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-extrabold text-sm flex items-center justify-center gap-2 shadow-lg shadow-emerald-950/50 transition-all hover:scale-[1.01] active:scale-[0.99]"
                >
                  {paying ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Đang xác nhận qua Kafka...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      <span>Xác Nhận Thanh Toán Thành Công (Demo PayOS)</span>
                    </>
                  )}
                </button>

                <p className="text-center text-[11px] text-slate-500">
                  (Bấm nút trên để giả lập thanh toán PayOS thành công, Kafka sẽ tự động xác nhận vé)
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
