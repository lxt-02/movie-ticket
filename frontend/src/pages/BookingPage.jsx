import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  Clock,
  ArrowLeft,
  ShieldCheck,
  AlertCircle,
  Ticket,
  CheckCircle2,
  Sparkles,
  Info
} from 'lucide-react';
import { getShowtimeSeats, createBooking, isAuthenticated } from '../api';

export default function BookingPage() {
  const { showtimeId } = useParams();
  const navigate = useNavigate();

  const [seats, setSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [countdown, setCountdown] = useState(600); // 10 minutes

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }

    setLoading(true);
    getShowtimeSeats(showtimeId)
      .then((data) => setSeats(data || []))
      .catch((err) => setErrorMsg(err.message || 'Không thể tải sơ đồ ghế'))
      .finally(() => setLoading(false));

    // 10-min countdown timer
    const timer = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [showtimeId, navigate]);

  const toggleSeat = (seat) => {
    if (seat.status !== 'AVAILABLE') return;
    if (selectedSeats.some((s) => s.id === seat.id)) {
      setSelectedSeats(selectedSeats.filter((s) => s.id !== seat.id));
    } else {
      if (selectedSeats.length >= 8) {
        alert('Mỗi giao dịch đặt vé được chọn tối đa 8 ghế!');
        return;
      }
      setSelectedSeats([...selectedSeats, seat]);
    }
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const totalPrice = selectedSeats.reduce((sum, s) => sum + (s.price || 95000), 0);

  const handleProceedToPayment = async () => {
    if (selectedSeats.length === 0) return;
    setBookingLoading(true);
    setErrorMsg('');

    try {
      const booking = await createBooking(showtimeId, selectedSeats);
      navigate(`/payment/${booking.id || booking.bookingId}`);
    } catch (err) {
      setErrorMsg(err.message || 'Đặt vé thất bại! Ghế đã được đặt hoặc bị giữ chỗ.');
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 animate-in fade-in duration-300 space-y-8">
      {/* Top Bar with Countdown Timer */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white border border-slate-200 rounded-3xl p-5 shadow-sm">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-xs font-bold text-slate-700 hover:text-red-600 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại chọn suất chiếu</span>
        </button>

        <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold">
          <Clock className="w-4 h-4 text-amber-600" />
          <span>Thời gian giữ ghế còn lại: </span>
          <span className="font-mono text-sm text-red-600">{formatTimer(countdown)}</span>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Seat Matrix Theater Layout */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-lg space-y-10 text-center">
        {/* Curved Screen Arc */}
        <div className="max-w-2xl mx-auto mb-12">
          <div className="h-2 w-full bg-gradient-to-r from-slate-200 via-red-500 to-slate-200 rounded-full shadow-md" />
          <p className="text-[11px] font-black tracking-widest text-slate-400 uppercase mt-2">
            MÀN HÌNH CHIẾU PHIM (SCREEN)
          </p>
        </div>

        {/* Seats Grid */}
        <div className="overflow-x-auto pb-6">
          <div className="inline-block min-w-[500px] space-y-3">
            {['A', 'B', 'C', 'D', 'E'].map((rowLetter) => {
              const rowSeats = seats.filter(
                (s) => s.rowCode === rowLetter || s.seatCode?.startsWith(rowLetter)
              );
              return (
                <div key={rowLetter} className="flex items-center justify-center gap-2.5">
                  <span className="w-6 font-bold text-slate-400 text-xs font-mono">{rowLetter}</span>
                  <div className="flex items-center gap-2">
                    {rowSeats.map((seat) => {
                      const isSelected = selectedSeats.some((s) => s.id === seat.id);
                      const isBooked = seat.status === 'BOOKED';
                      const isHeld = seat.status === 'HELD';
                      const isCouple = seat.seatType === 'COUPLE';
                      const isVip = seat.seatType === 'VIP';

                      let btnStyle = 'bg-slate-100 border-slate-300 text-slate-700 hover:bg-slate-200';
                      if (isVip) btnStyle = 'bg-amber-100 border-amber-300 text-amber-900 hover:bg-amber-200';
                      if (isCouple) btnStyle = 'bg-rose-100 border-rose-300 text-rose-900 hover:bg-rose-200 w-16';
                      if (isBooked) btnStyle = 'bg-slate-300 border-slate-300 text-slate-400 cursor-not-allowed';
                      if (isHeld) btnStyle = 'bg-amber-200 border-amber-300 text-amber-700 cursor-not-allowed';
                      if (isSelected) btnStyle = 'bg-red-600 border-red-600 text-white font-black shadow-md shadow-red-600/30 scale-105';

                      return (
                        <button
                          key={seat.id}
                          disabled={isBooked || isHeld}
                          onClick={() => toggleSeat(seat)}
                          className={`h-9 px-2 rounded-xl text-[11px] font-bold border transition-all flex items-center justify-center ${btnStyle} ${
                            isCouple ? 'w-16' : 'w-9'
                          }`}
                        >
                          {seat.seatCode || seat.seatLabel}
                        </button>
                      );
                    })}
                  </div>
                  <span className="w-6 font-bold text-slate-400 text-xs font-mono">{rowLetter}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Legend */}
        <div className="pt-6 border-t border-slate-100 flex flex-wrap items-center justify-center gap-6 text-xs text-slate-600">
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-md bg-slate-100 border border-slate-300" />
            <span>Ghế Thường</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-md bg-amber-100 border border-amber-300" />
            <span>Ghế VIP</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-6 h-4 rounded-md bg-rose-100 border border-rose-300" />
            <span>Ghế Đôi SweetBox</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-md bg-red-600 text-white" />
            <span>Đang Chọn</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-md bg-slate-300 border border-slate-300" />
            <span>Đã Bán</span>
          </div>
        </div>
      </div>

      {/* Floating Checkout Bar */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-xl flex flex-col sm:flex-row items-center justify-between gap-6">
        <div>
          <span className="text-xs text-slate-500 block mb-1">Ghế đã chọn:</span>
          <div className="font-black text-slate-900 text-lg">
            {selectedSeats.length === 0 ? (
              <span className="text-slate-400 font-normal text-sm">Vui lòng chọn ghế trên sơ đồ</span>
            ) : (
              selectedSeats.map((s) => s.seatCode || s.seatLabel).join(', ')
            )}
          </div>
        </div>

        <div className="flex items-center gap-6">
          <div className="text-right">
            <span className="text-xs text-slate-500 block mb-0.5">Tổng số tiền:</span>
            <span className="text-2xl font-black text-red-600 font-mono">
              {totalPrice.toLocaleString()} VND
            </span>
          </div>

          <button
            onClick={handleProceedToPayment}
            disabled={selectedSeats.length === 0 || bookingLoading}
            className="px-8 py-3.5 rounded-2xl bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-black text-sm uppercase tracking-wider flex items-center gap-2 shadow-lg shadow-red-600/30 hover:scale-105 active:scale-95 transition-all"
          >
            <Ticket className="w-4 h-4" />
            <span>{bookingLoading ? 'Đang tạo đơn...' : 'Tiếp Tục Thanh Toán'}</span>
          </button>
        </div>
      </div>
    </div>
  );
}