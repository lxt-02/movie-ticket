import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Ticket,
  Clock,
  MapPin,
  Calendar,
  AlertTriangle,
  Printer,
  XCircle,
  CheckCircle2,
  ArrowRight,
  Sparkles
} from 'lucide-react';
import { getUserBookings, cancelBooking, isAuthenticated } from '../api';

export default function MyTicketsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState('');
  const navigate = useNavigate();

  const loadBookings = () => {
    setLoading(true);
    getUserBookings()
      .then((data) => setBookings(Array.isArray(data) ? data : data?.content || []))
      .catch(() => setBookings([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    loadBookings();
  }, [navigate]);

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn đặt vé này không?')) return;
    setActionLoading(true);
    setActionMsg('');

    try {
      await cancelBooking(bookingId);
      setActionMsg('Đã hủy vé thành công!');
      loadBookings();
    } catch (err) {
      alert(err.message || 'Hủy vé thất bại');
    } finally {
      setActionLoading(false);
    }
  };

  const sampleBookings = [
    {
      id: '90000000-0000-0000-0000-000000000001',
      bookingCode: 'BK888999',
      movieTitle: 'BÒ SỮA BAY',
      cinemaName: 'MTK Landmark 81',
      screenName: 'Phòng 1 (IMAX Laser)',
      startTime: '2026-11-10T19:30:00Z',
      seats: [{ seatLabel: 'C4' }, { seatLabel: 'C5' }],
      totalAmount: 230000,
      status: 'CONFIRMED',
      createdAt: '2026-11-10T10:00:00Z',
    },
    {
      id: '90000000-0000-0000-0000-000000000002',
      bookingCode: 'BK555666',
      movieTitle: 'THIÊN ĐƯỜNG TĂM TỐI',
      cinemaName: 'MTK Vincom Đồng Khởi',
      screenName: 'Phòng 2 (4DX)',
      startTime: '2026-11-09T21:00:00Z',
      seats: [{ seatLabel: 'D3' }],
      totalAmount: 125000,
      status: 'CANCELLED',
      createdAt: '2026-11-09T14:30:00Z',
    },
  ];

  const displayList = bookings.length > 0 ? bookings : sampleBookings;

  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-10 animate-in fade-in duration-300 space-y-8">
      {/* Header */}
      <div className="bg-gradient-to-r from-red-600 to-rose-700 rounded-3xl p-8 sm:p-12 text-white shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-white/20 inline-block mb-3">
            LỊCH SỬ ĐẶT VÉ CỦA BẠN
          </span>
          <h1 className="text-3xl sm:text-4xl font-black uppercase tracking-tight">
            VÉ CỦA TÔI
          </h1>
          <p className="text-white/80 text-sm mt-2">
            Quản lý toàn bộ vé xem phim điện tử và xuất trình mã QR check-in tại rạp
          </p>
        </div>

        <Link
          to="/movies"
          className="px-6 py-3 rounded-2xl bg-white hover:bg-slate-100 text-red-600 font-black text-xs uppercase tracking-wider flex items-center gap-2 shadow-lg transition-all"
        >
          <Ticket className="w-4 h-4" />
          <span>Đặt Thêm Vé Mới</span>
        </Link>
      </div>

      {actionMsg && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{actionMsg}</span>
        </div>
      )}

      {/* List */}
      {loading ? (
        <div className="py-20 text-center">
          <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin mx-auto" />
        </div>
      ) : displayList.length === 0 ? (
        <div className="p-16 text-center bg-white border border-slate-200 rounded-3xl shadow-sm space-y-4">
          <Ticket className="w-12 h-12 text-slate-300 mx-auto" />
          <h3 className="text-lg font-bold text-slate-800">Bạn chưa có đơn đặt vé nào</h3>
          <p className="text-xs text-slate-500">Khám phá các bom tấn đang chiếu và đặt ngay những vị trí ghế đẹp nhất!</p>
          <Link
            to="/movies"
            className="inline-block px-6 py-2.5 rounded-xl bg-red-600 text-white font-bold text-xs"
          >
            Duyệt Phim Ngay
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {displayList.map((b) => {
            const isConfirmed = b.status === 'CONFIRMED' || b.status === 'PAID';
            const isCancelled = b.status === 'CANCELLED';

            return (
              <div
                key={b.id}
                className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm hover:shadow-md transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-6"
              >
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-black text-red-600 bg-red-50 px-2.5 py-0.5 rounded-lg border border-red-200">
                      {b.bookingCode || 'BK' + b.id.slice(0, 6)}
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-lg text-[10px] font-black uppercase ${
                        isConfirmed
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : isCancelled
                          ? 'bg-slate-100 text-slate-500 border border-slate-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}
                    >
                      {isConfirmed ? 'Đã Thanh Toán' : isCancelled ? 'Đã Hủy Vé' : 'Chờ Thanh Toán'}
                    </span>
                  </div>

                  <h3 className="text-xl font-black text-slate-900 uppercase">
                    {b.movieTitle}
                  </h3>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-red-600" />
                      {b.cinemaName} • {b.screenName}
                    </span>
                    <span className="flex items-center gap-1 font-mono">
                      <Clock className="w-3.5 h-3.5 text-red-600" />
                      {b.startTime ? new Date(b.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '19:30'}
                    </span>
                    <span className="font-bold text-red-600">
                      Ghế:{' '}
                      {Array.isArray(b.seats)
                        ? b.seats.map((s) => s.seatLabel).join(', ')
                        : 'A1, A2'}
                    </span>
                  </div>
                </div>

                {/* Right Actions */}
                <div className="flex items-center gap-3 w-full md:w-auto justify-end pt-4 md:pt-0 border-t md:border-t-0 border-slate-100">
                  <div className="text-right pr-4 hidden sm:block">
                    <span className="text-xs text-slate-400 block">Tổng tiền</span>
                    <span className="text-base font-black text-slate-900 font-mono">
                      {Number(b.totalAmount || 190000).toLocaleString()} VND
                    </span>
                  </div>

                  <Link
                    to={`/payment/${b.id}`}
                    className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs flex items-center gap-1.5 transition-all"
                  >
                    <Ticket className="w-4 h-4 text-red-400" />
                    <span>Xem Cuống Vé</span>
                  </Link>

                  {isConfirmed && (
                    <button
                      onClick={() => handleCancelBooking(b.id)}
                      disabled={actionLoading}
                      className="px-4 py-2.5 rounded-xl bg-red-50 hover:bg-red-100 border border-red-200 text-red-700 font-bold text-xs flex items-center gap-1.5 transition-all"
                    >
                      <XCircle className="w-4 h-4" />
                      <span>Hủy Vé</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}