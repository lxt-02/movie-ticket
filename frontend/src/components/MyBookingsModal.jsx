import React, { useState, useEffect } from 'react';
import { X, Ticket, Clock, CheckCircle2, AlertCircle, RefreshCw } from 'lucide-react';
import { getUserBookings } from '../api';

export default function MyBookingsModal({ onClose }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchBookings = () => {
    setLoading(true);
    getUserBookings()
      .then((data) => setBookings(Array.isArray(data) ? data : []))
      .catch((e) => console.log('Error fetch bookings:', e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/50">
          <div className="flex items-center gap-2">
            <Ticket className="w-5 h-5 text-rose-400" />
            <h3 className="text-base font-bold text-white">Lịch Sử Đặt Vé Của Bạn</h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4">
          {loading ? (
            <div className="text-center py-12 text-sm text-slate-400">Đang tải danh sách vé...</div>
          ) : bookings.length === 0 ? (
            <div className="text-center py-12 bg-slate-950/50 rounded-2xl border border-slate-800">
              <Ticket className="w-10 h-10 text-slate-600 mx-auto mb-2" />
              <p className="text-sm font-semibold text-slate-300">Bạn chưa có đơn đặt vé nào.</p>
              <p className="text-xs text-slate-500 mt-1">Hãy chọn phim và trải nghiệm đặt vé ngay!</p>
            </div>
          ) : (
            bookings.map((b) => {
              const isConfirmed = b.status === 'CONFIRMED';
              const isPending = b.status === 'PENDING';
              const isExpired = b.status === 'EXPIRED' || b.status === 'CANCELLED';

              return (
                <div
                  key={b.id}
                  className="p-4 rounded-2xl bg-slate-950 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-slate-200">
                        #{b.bookingCode || b.id.substring(0, 8)}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-extrabold uppercase ${
                          isConfirmed
                            ? 'bg-emerald-950 text-emerald-300 border border-emerald-700'
                            : isPending
                            ? 'bg-amber-950 text-amber-300 border border-amber-700 animate-pulse'
                            : 'bg-rose-950 text-rose-300 border border-rose-800'
                        }`}
                      >
                        {b.status}
                      </span>
                    </div>

                    <div className="text-xs text-slate-400">
                      Ghế: <span className="font-bold text-slate-200">{(b.items || []).map((i) => i.seatCode || i.seatNumber).join(', ') || 'Ghế mẫu'}</span>
                    </div>

                    <div className="text-[11px] text-slate-500">
                      Tạo lúc: {b.createdAt ? new Date(b.createdAt).toLocaleString('vi-VN') : 'Vừa xong'}
                    </div>
                  </div>

                  <div className="text-right sm:border-l sm:border-slate-800 sm:pl-4">
                    <span className="text-[11px] text-slate-500 block">Tổng tiền</span>
                    <span className="text-base font-black text-rose-400">
                      {(b.finalAmount || b.totalAmount || 95000).toLocaleString('vi-VN')} đ
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
