import React, { useState, useEffect } from 'react';
import { X, Clock, Armchair, AlertTriangle, ArrowRight, ShieldCheck } from 'lucide-react';
import { getShowtimeSeats, createBooking } from '../api';

export default function SeatMatrixModal({ showtime, movie, onClose, onBookingSuccess }) {
  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [timeLeft, setTimeLeft] = useState(600); // 10 minutes timer in seconds

  useEffect(() => {
    if (!showtime) return;
    setLoading(true);
    getShowtimeSeats(showtime.id)
      .then((data) => setSeats(data || []))
      .catch((e) => setErrorMsg(e.message))
      .finally(() => setLoading(false));

    // Countdown 10 minutes
    const interval = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [showtime]);

  const toggleSelectSeat = (seat) => {
    if (seat.status !== 'AVAILABLE') return;
    setErrorMsg('');
    if (selectedSeatIds.includes(seat.id)) {
      setSelectedSeatIds(selectedSeatIds.filter((id) => id !== seat.id));
    } else {
      if (selectedSeatIds.length >= 8) {
        setErrorMsg('Bạn chỉ được chọn tối đa 8 ghế mỗi lần đặt.');
        return;
      }
      setSelectedSeatIds([...selectedSeatIds, seat.id]);
    }
  };

  const selectedSeats = seats.filter((s) => selectedSeatIds.includes(s.id));
  const totalPrice = selectedSeats.reduce((sum, s) => sum + (s.price || showtime.basePrice || 95000), 0);

  const formatTimer = (sec) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const handleConfirmBooking = async () => {
    if (selectedSeatIds.length === 0) {
      setErrorMsg('Vui lòng chọn ít nhất 1 ghế trước khi đặt.');
      return;
    }
    setBookingLoading(true);
    setErrorMsg('');
    try {
      const booking = await createBooking(showtime.id, selectedSeatIds);
      onBookingSuccess(booking, selectedSeats, showtime, movie);
    } catch (err) {
      setErrorMsg(err.message || 'Ghế đã bị người khác chọn hoặc lỗi kết nối.');
    } finally {
      setBookingLoading(false);
    }
  };

  // Group seats by rowCode
  const rowCodes = Array.from(new Set(seats.map((s) => s.rowCode || s.seatCode?.[0] || 'A'))).sort();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-4xl bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden max-h-[95vh] flex flex-col">
        {/* Top Header */}
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-950/50">
          <div>
            <h2 className="text-lg font-black text-white flex items-center gap-2">
              <span>{movie.title}</span>
              <span className="text-xs px-2 py-0.5 rounded bg-rose-600/30 text-rose-400 border border-rose-500/30">
                {showtime.startTime || '19:30'}
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              {showtime.cinemaName || 'CineStar Quốc Thanh'} • {showtime.screenName || 'Phòng 1'}
            </p>
          </div>

          <div className="flex items-center gap-4">
            {/* Live Hold Timer */}
            <div className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs font-bold font-mono">
              <Clock className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
              <span>{formatTimer(timeLeft)}</span>
            </div>

            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Seat Matrix Area */}
        <div className="p-6 overflow-y-auto flex-1 flex flex-col items-center">
          {/* Cinema Screen Curve */}
          <div className="w-full max-w-xl mb-10 flex flex-col items-center">
            <div className="w-full h-3 rounded-t-[100px] bg-gradient-to-b from-rose-500/60 via-rose-500/20 to-transparent shadow-[0_-8px_20px_rgba(244,63,94,0.3)] mb-2" />
            <span className="text-[10px] uppercase font-bold tracking-[0.25em] text-slate-500">Màn Hình Chiếu</span>
          </div>

          {/* Error Notice */}
          {errorMsg && (
            <div className="w-full max-w-xl mb-4 p-3 rounded-xl bg-rose-950/50 border border-rose-800/80 text-rose-300 text-xs flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Seat Grid */}
          {loading ? (
            <div className="py-20 text-sm text-slate-400">Đang nạp sơ đồ ghế thực tế...</div>
          ) : (
            <div className="space-y-3 mb-8 overflow-x-auto max-w-full pb-2">
              {rowCodes.map((row) => (
                <div key={row} className="flex items-center justify-center gap-2">
                  <span className="w-5 text-xs font-bold text-slate-500 text-right">{row}</span>
                  <div className="flex gap-2">
                    {seats
                      .filter((s) => (s.rowCode || s.seatCode?.[0]) === row)
                      .map((seat) => {
                        const isSelected = selectedSeatIds.includes(seat.id);
                        const isBooked = seat.status === 'BOOKED';
                        const isHeld = seat.status === 'HELD';
                        const isVip = seat.seatType === 'VIP';
                        const isCouple = seat.seatType === 'COUPLE';

                        let btnClasses = 'w-8 h-8 rounded-lg text-[10px] font-bold flex items-center justify-center transition-all ';
                        if (isBooked) {
                          btnClasses += 'bg-slate-800 text-slate-600 cursor-not-allowed border border-slate-800';
                        } else if (isHeld) {
                          btnClasses += 'bg-amber-900/40 text-amber-500 cursor-not-allowed border border-amber-600/50';
                        } else if (isSelected) {
                          btnClasses += 'bg-rose-500 text-white shadow-lg shadow-rose-600/50 scale-105 border border-rose-400';
                        } else if (isCouple) {
                          btnClasses += 'w-16 bg-pink-950/60 hover:bg-pink-900/80 text-pink-300 border border-pink-700/60';
                        } else if (isVip) {
                          btnClasses += 'bg-amber-950/50 hover:bg-amber-900/70 text-amber-300 border border-amber-600/60';
                        } else {
                          btnClasses += 'bg-slate-800/80 hover:bg-slate-700 text-slate-200 border border-slate-700/60';
                        }

                        return (
                          <button
                            key={seat.id}
                            disabled={isBooked || isHeld}
                            onClick={() => toggleSelectSeat(seat)}
                            title={`${seat.seatCode} (${seat.seatType}) - ${(seat.price || 95000).toLocaleString('vi-VN')}đ`}
                            className={btnClasses}
                          >
                            {seat.seatCode}
                          </button>
                        );
                      })}
                  </div>
                  <span className="w-5 text-xs font-bold text-slate-500 text-left">{row}</span>
                </div>
              ))}
            </div>
          )}

          {/* Seat Legends */}
          <div className="flex flex-wrap items-center justify-center gap-4 text-xs text-slate-400 pt-4 border-t border-slate-800 w-full max-w-xl">
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-slate-800 border border-slate-700" />
              <span>Ghế thường</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-amber-950/50 border border-amber-600" />
              <span>Ghế VIP</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-6 h-4 rounded bg-pink-950/60 border border-pink-700" />
              <span>Ghế Đôi</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-rose-500 text-white" />
              <span>Bạn chọn</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-amber-900/40 border border-amber-600/50" />
              <span>Đang giữ</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-slate-800 border border-slate-800 text-slate-600" />
              <span>Đã bán</span>
            </div>
          </div>
        </div>

        {/* Bottom Booking Checkout Bar */}
        <div className="px-6 py-4 bg-slate-950 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <div className="text-xs text-slate-400">
              Ghế đang chọn:{' '}
              <span className="font-bold text-rose-400">
                {selectedSeats.length > 0
                  ? selectedSeats.map((s) => s.seatCode).join(', ')
                  : 'Chưa chọn ghế nào'}
              </span>
            </div>
            <div className="text-lg font-black text-white">
              Tổng tiền: <span className="text-rose-400">{totalPrice.toLocaleString('vi-VN')} đ</span>
            </div>
          </div>

          <button
            disabled={selectedSeatIds.length === 0 || bookingLoading}
            onClick={handleConfirmBooking}
            className="w-full sm:w-auto px-6 py-3 rounded-xl bg-gradient-to-r from-rose-600 to-pink-600 hover:from-rose-500 hover:to-pink-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-rose-900/40 transition-all"
          >
            {bookingLoading ? (
              <span>Đang khóa ghế & tạo đơn...</span>
            ) : (
              <>
                <span>Tiến Hành Đặt Vé</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
