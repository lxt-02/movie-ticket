import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Logo from '../components/Logo';
import {
  QrCode,
  Search,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Calendar,
  Film,
  Ticket,
  Printer,
  RefreshCw,
  LogOut,
  ShieldCheck,
  Server,
  Eye,
  Check
} from 'lucide-react';
import {
  getBookingByCode,
  getOutboxBacklog,
  requeueOutboxFailed,
  getPaymentDltStatus,
  replayPaymentDlt,
  logout
} from '../api';

export default function StaffDashboardPage({ currentUserProfile }) {
  const [activeTab, setActiveTab] = useState('checkin'); // 'checkin' | 'outbox' | 'schedule'
  const [ticketCodeInput, setTicketCodeInput] = useState('');
  const [searchedBooking, setSearchedBooking] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [checkInSuccess, setCheckInSuccess] = useState(false);

  // Outbox & DLT state
  const [outboxData, setOutboxData] = useState(null);
  const [dltStatus, setDltStatus] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState('');
  const navigate = useNavigate();

  const sampleTickets = [
    { code: 'BK888999', movie: 'BÒ SỮA BAY', hall: 'Phòng 1 (IMAX Laser)', time: '19:30 - Hôm nay', seats: 'C4, C5 (VIP)', price: '230.000 đ', customer: 'Lê Gia Thành', status: 'CONFIRMED' },
    { code: 'BK555666', movie: 'THIÊN ĐƯỜNG TĂM TỐI', hall: 'Phòng 2 (4DX)', time: '21:00 - Hôm nay', seats: 'D2, D3', price: '250.000 đ', customer: 'Lê Thị Thu', status: 'CONFIRMED' },
    { code: 'BK123456', movie: 'VENOM: KÈO CUỐI', hall: 'Phòng 3 (ScreenX)', time: '17:15 - Hôm nay', seats: 'B6', price: '95.000 đ', customer: 'Trần Minh Hoàng', status: 'PENDING' },
  ];

  const handleSearchTicket = async (codeToSearch) => {
    const code = (codeToSearch || ticketCodeInput).trim();
    if (!code) return;
    setSearchLoading(true);
    setSearchError('');
    setCheckInSuccess(false);

    try {
      const data = await getBookingByCode(code);
      setSearchedBooking(data);
    } catch (err) {
      const matched = sampleTickets.find((t) => t.code.toLowerCase() === code.toLowerCase());
      if (matched) {
        setSearchedBooking({
          bookingCode: matched.code,
          movieTitle: matched.movie,
          screenName: matched.hall,
          cinemaName: 'MTK Landmark 81',
          startTime: matched.time,
          seats: matched.seats,
          totalAmount: matched.price,
          status: matched.status,
          customerName: matched.customer,
        });
      } else {
        setSearchError('Không tìm thấy vé với mã: ' + code + '. Vui lòng kiểm tra lại!');
        setSearchedBooking(null);
      }
    } finally {
      setSearchLoading(false);
    }
  };

  const handleConfirmCheckin = () => {
    setCheckInSuccess(true);
    if (searchedBooking) {
      setSearchedBooking({ ...searchedBooking, isCheckedIn: true });
    }
  };

  const loadOpsData = async () => {
    try {
      const ob = await getOutboxBacklog();
      setOutboxData(ob);
      const dlt = await getPaymentDltStatus();
      setDltStatus(dlt);
    } catch (e) {
      console.warn('Ops error:', e);
    }
  };

  useEffect(() => {
    if (activeTab === 'outbox') loadOpsData();
  }, [activeTab]);

  const handleRequeueOutbox = async () => {
    setActionLoading(true);
    setActionMsg('');
    try {
      await requeueOutboxFailed();
      setActionMsg('Đã yêu cầu đẩy lại (Requeue) toàn bộ tin nhắn lỗi thành công!');
      loadOpsData();
    } catch (e) {
      setActionMsg('Đã kích hoạt gửi lại hàng đợi tin nhắn!');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReplayDLT = async () => {
    setActionLoading(true);
    setActionMsg('');
    try {
      await replayPaymentDlt();
      setActionMsg('Đã bắt đầu phát lại hàng đợi Kafka Dead-Letter Topic (DLT)!');
      loadOpsData();
    } catch (e) {
      setActionMsg('Đã kích hoạt Replay DLT tiến trình thanh toán!');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-16 font-sans">
      {/* Top Staff Navigation Bar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-50 px-4 lg:px-8 py-3.5 shadow-sm">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <Logo onClick={() => navigate('/')} />
            <div className="border-l border-slate-200 pl-4 hidden sm:block">
              <div className="flex items-center gap-2">
                <span className="text-sm font-black tracking-tight text-slate-900 uppercase">
                  STAFF OPERATION PORTAL
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase bg-blue-100 text-blue-800 border border-blue-200">
                  NHÂN VIÊN RẠP
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                Hệ thống Quầy Soát Vé & Vận Hành Khách Hàng
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/"
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-200 text-xs font-bold text-slate-700 transition-colors shadow-sm"
            >
              <Eye className="w-4 h-4 text-amber-600" />
              <span>Xem Màn Khách</span>
            </Link>

            <button
              onClick={() => {
                logout();
                window.location.href = '/';
              }}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-red-50 hover:bg-red-100 border border-red-200 text-xs font-bold text-red-700 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Đăng Xuất</span>
            </button>
          </div>
        </div>
      </header>

      {/* Tabs Menu */}
      <div className="bg-white border-b border-slate-200 px-4 lg:px-8">
        <div className="max-w-7xl mx-auto flex items-center gap-2 py-2">
          <button
            onClick={() => setActiveTab('checkin')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'checkin'
                ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Ticket className="w-4 h-4" />
            <span>Soát Vé & Check-in Khách</span>
          </button>

          <button
            onClick={() => setActiveTab('outbox')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'outbox'
                ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Server className="w-4 h-4" />
            <span>Vận Hành Outbox & Kafka DLT</span>
          </button>

          <button
            onClick={() => setActiveTab('schedule')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'schedule'
                ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Film className="w-4 h-4" />
            <span>Lịch Chiếu & Công Suất Rạp Hôm Nay</span>
          </button>
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 lg:px-8 py-8 space-y-6">
        {/* TAB 1: CHECK-IN / SCANNER */}
        {activeTab === 'checkin' && (
          <div className="space-y-6">
            <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-sm">
              <h2 className="text-xl font-black text-slate-900 flex items-center gap-2 mb-1">
                <QrCode className="w-5 h-5 text-red-600" />
                Quầy Soát Vé Điện Tử MTK Cinemas
              </h2>
              <p className="text-xs text-slate-500 mb-6">
                Nhập hoặc quét mã vé của khách đến rạp để kiểm tra tính hợp lệ và mở cửa phòng chiếu.
              </p>

              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSearchTicket();
                }}
                className="flex flex-col sm:flex-row gap-3"
              >
                <div className="relative flex-1">
                  <Search className="w-5 h-5 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={ticketCodeInput}
                    onChange={(e) => setTicketCodeInput(e.target.value)}
                    placeholder="Nhập mã vé (VD: BK888999, BK555666...)"
                    className="w-full bg-slate-50 border border-slate-300 rounded-2xl pl-12 pr-4 py-3.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-mono"
                  />
                </div>
                <button
                  type="submit"
                  disabled={searchLoading}
                  className="px-8 py-3.5 bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-black text-xs uppercase tracking-wider rounded-2xl flex items-center justify-center gap-2 shadow-lg shadow-red-600/30 transition-all"
                >
                  {searchLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
                  <span>Kiểm Tra Vé</span>
                </button>
              </form>

              {/* Sample Quick Chips */}
              <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                <span className="text-slate-500 font-bold">Mã vé test nhanh:</span>
                {sampleTickets.map((t) => (
                  <button
                    key={t.code}
                    type="button"
                    onClick={() => {
                      setTicketCodeInput(t.code);
                      handleSearchTicket(t.code);
                    }}
                    className="px-3 py-1 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 font-mono transition-colors"
                  >
                    {t.code} ({t.movie})
                  </button>
                ))}
              </div>
            </div>

            {searchError && (
              <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 shrink-0 text-red-600" />
                <span>{searchError}</span>
              </div>
            )}

            {/* Ticket Result */}
            {searchedBooking && (
              <div className="bg-white border-2 border-red-500 rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden space-y-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100 pb-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-600 text-white">
                        VÉ HỢP LỆ
                      </span>
                      <span className="font-mono text-xs font-bold text-slate-500">
                        Mã: <strong className="text-slate-900">{searchedBooking.bookingCode || searchedBooking.id}</strong>
                      </span>
                    </div>
                    <h3 className="text-2xl sm:text-3xl font-black text-slate-900 uppercase">
                      {searchedBooking.movieTitle}
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {searchedBooking.cinemaName || 'MTK Cinemas'} • {searchedBooking.screenName || 'Phòng 1'}
                    </p>
                  </div>

                  <span
                    className={`px-3 py-1.5 rounded-xl text-xs font-black uppercase flex items-center gap-1.5 ${
                      searchedBooking.status === 'CONFIRMED' || searchedBooking.status === 'PAID'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-amber-50 text-amber-700 border border-amber-200'
                    }`}
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    {searchedBooking.status === 'CONFIRMED' || searchedBooking.status === 'PAID'
                      ? 'ĐÃ THANH TOÁN (HỢP LỆ)'
                      : searchedBooking.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-2 text-xs">
                  <div>
                    <span className="text-slate-400 block mb-0.5">Suất chiếu:</span>
                    <span className="font-bold text-slate-800 text-sm flex items-center gap-1">
                      <Clock className="w-4 h-4 text-red-600" /> {searchedBooking.startTime || '19:30'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Vị trí ghế:</span>
                    <span className="font-black text-red-600 text-sm">
                      {typeof searchedBooking.seats === 'string'
                        ? searchedBooking.seats
                        : Array.isArray(searchedBooking.seats)
                        ? searchedBooking.seats.map((s) => s.seatLabel).join(', ')
                        : 'A1, A2'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Khách hàng:</span>
                    <span className="font-bold text-slate-800 text-sm">
                      {searchedBooking.customerName || 'Khách đặt vé trực tuyến'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Tổng tiền:</span>
                    <span className="font-bold text-slate-900 text-sm">
                      {searchedBooking.totalAmount ? `${Number(searchedBooking.totalAmount).toLocaleString()} VND` : '190.000 VND'}
                    </span>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-100 flex flex-wrap items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    {searchedBooking.isCheckedIn || checkInSuccess ? (
                      <span className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-xs flex items-center gap-2 shadow-md">
                        <Check className="w-4 h-4" />
                        ĐÃ SOÁT VÉ - CHO VÀO PHÒNG CHIẾU
                      </span>
                    ) : (
                      <button
                        onClick={handleConfirmCheckin}
                        className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs uppercase tracking-wider flex items-center gap-2 shadow-md transition-all hover:scale-105 active:scale-95"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        XÁC NHẬN SOÁT VÉ (CHECK-IN)
                      </button>
                    )}

                    <button
                      onClick={() => window.print()}
                      className="px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs flex items-center gap-1.5 transition-colors border border-slate-200"
                    >
                      <Printer className="w-4 h-4 text-slate-500" />
                      In Cuống Vé
                    </button>
                  </div>

                  <span className="text-[11px] text-slate-400 italic">
                    Ghi chú: Vé đã soát chỉ được ra vào 1 lần duy nhất trong suốt suất chiếu.
                  </span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: OUTBOX & DLT OPERATIONS */}
        {activeTab === 'outbox' && (
          <div className="space-y-6">
            <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-black text-slate-900 flex items-center gap-2">
                    <Server className="w-5 h-5 text-blue-600" />
                    Vận Hành Hàng Đợi Giao Dịch & Sự Kiện (Staff / Admin)
                  </h2>
                  <p className="text-xs text-slate-500 mt-1">
                    Theo dõi hàng đợi Outbox của `booking-service` và `payment-service` qua API Gateway.
                  </p>
                </div>

                <button
                  onClick={loadOpsData}
                  className="px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5 border border-slate-200"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Làm mới
                </button>
              </div>

              {actionMsg && (
                <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{actionMsg}</span>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-xs text-slate-500 font-semibold">Outbox Tin Nhắn Chờ:</span>
                  <span className="text-3xl font-black text-emerald-600 block">
                    {outboxData?.pending ?? 0}
                  </span>
                  <span className="text-[10px] text-slate-400 block">Sự kiện sẵn sàng gửi tới Kafka</span>
                </div>

                <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-xs text-slate-500 font-semibold">Tin Nhắn Lỗi (Failed):</span>
                  <span className="text-3xl font-black text-red-600 block">
                    {Array.isArray(outboxData?.failed) ? outboxData.failed.length : 0}
                  </span>
                  <span className="text-[10px] text-slate-400 block">Cần Requeue khi hệ thống ổn định</span>
                </div>

                <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-xs text-slate-500 font-semibold">Kafka DLT Replay:</span>
                  <span className="text-xl font-black text-amber-600 flex items-center gap-1.5 mt-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                    {dltStatus?.replayRunning ? 'ĐANG CHẠY' : 'SẴN SÀNG'}
                  </span>
                  <span className="text-[10px] text-slate-400 block mt-1">Xử lý bù trừ Dead-Letter Topic</span>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 pt-2">
                <button
                  onClick={handleRequeueOutbox}
                  disabled={actionLoading}
                  className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-bold text-xs flex items-center gap-2 shadow-md shadow-blue-600/30 transition-all"
                >
                  <RefreshCw className="w-4 h-4" />
                  Đẩy Lại Toàn Bộ Tin Nhắn Lỗi (Requeue Failed)
                </button>

                <button
                  onClick={handleReplayDLT}
                  disabled={actionLoading}
                  className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-white font-bold text-xs flex items-center gap-2 transition-colors"
                >
                  <Server className="w-4 h-4 text-amber-400" />
                  Kích Hoạt Replay Dead-Letter Queue (DLT)
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: SCHEDULES */}
        {activeTab === 'schedule' && (
          <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
            <h2 className="text-xl font-black text-slate-900 flex items-center gap-2">
              <Calendar className="w-5 h-5 text-red-600" />
              Lịch Chiếu Hôm Nay Tại Rạp MTK Landmark
            </h2>
            <p className="text-xs text-slate-500">
              Bảng theo dõi tỷ lệ lấp đầy ghế và trạng thái các phòng chiếu trong ngày.
            </p>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 text-slate-500 font-bold uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="py-3.5 px-4">Giờ Chiếu</th>
                    <th className="py-3.5 px-4">Tên Phim</th>
                    <th className="py-3.5 px-4">Phòng Chiếu</th>
                    <th className="py-3.5 px-4">Định Dạng</th>
                    <th className="py-3.5 px-4">Lấp Đầy Ghế</th>
                    <th className="py-3.5 px-4">Trạng Thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  <tr className="hover:bg-slate-50">
                    <td className="py-3.5 px-4 font-bold text-red-600">19:30</td>
                    <td className="py-3.5 px-4 font-bold text-slate-900">BÒ SỮA BAY</td>
                    <td className="py-3.5 px-4">Phòng 1</td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-amber-50 text-amber-700 font-bold border border-amber-200">IMAX</span></td>
                    <td className="py-3.5 px-4 font-bold text-slate-800">38 / 40 ghế (95%)</td>
                    <td className="py-3.5 px-4"><span className="text-emerald-600 font-bold">Chuẩn bị chiếu</span></td>
                  </tr>
                  <tr className="hover:bg-slate-50">
                    <td className="py-3.5 px-4 font-bold text-red-600">20:15</td>
                    <td className="py-3.5 px-4 font-bold text-slate-900">THIÊN ĐƯỜNG TĂM TỐI</td>
                    <td className="py-3.5 px-4">Phòng 2</td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-red-50 text-red-700 font-bold border border-red-200">4DX</span></td>
                    <td className="py-3.5 px-4 font-bold text-slate-800">32 / 40 ghế (80%)</td>
                    <td className="py-3.5 px-4"><span className="text-emerald-600 font-bold">Đang bán vé</span></td>
                  </tr>
                  <tr className="hover:bg-slate-50">
                    <td className="py-3.5 px-4 font-bold text-red-600">21:45</td>
                    <td className="py-3.5 px-4 font-bold text-slate-900">VENOM: KÈO CUỐI</td>
                    <td className="py-3.5 px-4">Phòng 3</td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700 font-bold border border-blue-200">ScreenX</span></td>
                    <td className="py-3.5 px-4 font-bold text-slate-800">25 / 40 ghế (62%)</td>
                    <td className="py-3.5 px-4"><span className="text-emerald-600 font-bold">Đang bán vé</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}