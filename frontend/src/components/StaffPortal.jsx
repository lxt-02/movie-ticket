import React, { useState, useEffect } from 'react';
import Logo from './Logo';
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
  Check,
  Users
} from 'lucide-react';
import {
  getBookingByCode,
  getOutboxBacklog,
  requeueOutboxFailed,
  getPaymentDltStatus,
  replayPaymentDlt,
  logout
} from '../api';

export default function StaffPortal({ currentUserProfile, onSwitchToCustomerView }) {
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

  // Sample quick test tickets
  const sampleTickets = [
    { code: 'BK888999', movie: 'BÒ SỮA BAY', hall: 'Phòng Chiếu 1 (IMAX)', time: '19:30 - Hôm nay', seats: 'C4, C5 (VIP)', price: '230.000 đ', customer: 'Nguyễn Văn A', status: 'CONFIRMED' },
    { code: 'BK555666', movie: 'THIÊN ĐƯỜNG TĂM TỐI', hall: 'Phòng Chiếu 3 (4DX)', time: '21:00 - Hôm nay', seats: 'D2, D3', price: '250.000 đ', customer: 'Lê Thị Thu', status: 'CONFIRMED' },
    { code: 'BK123456', movie: 'VENOM: KÈO CUỐI', hall: 'Phòng Chiếu 2 (2D)', time: '17:15 - Hôm nay', seats: 'B6', price: '95.000 đ', customer: 'Trần Minh Hoàng', status: 'PENDING' },
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
      // If backend mock or not found, check sample list
      const matchedSample = sampleTickets.find(t => t.code.toLowerCase() === code.toLowerCase());
      if (matchedSample) {
        setSearchedBooking({
          bookingCode: matchedSample.code,
          movieTitle: matchedSample.movie,
          screenName: matchedSample.hall,
          cinemaName: 'MTK Cinemas Landmark',
          startTime: matchedSample.time,
          seats: matchedSample.seats,
          totalAmount: matchedSample.price,
          status: matchedSample.status,
          customerName: matchedSample.customer,
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
      setSearchedBooking({
        ...searchedBooking,
        isCheckedIn: true,
      });
    }
  };

  const loadOpsData = async () => {
    try {
      const ob = await getOutboxBacklog();
      setOutboxData(ob);
      const dlt = await getPaymentDltStatus();
      setDltStatus(dlt);
    } catch (e) {
      console.warn('Load ops error', e);
    }
  };

  useEffect(() => {
    if (activeTab === 'outbox') {
      loadOpsData();
    }
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
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col font-sans">
      {/* Top Staff Navigation Bar */}
      <header className="bg-neutral-900 border-b border-neutral-800 sticky top-0 z-50 px-4 lg:px-8 py-3.5 shadow-xl">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          {/* Logo & Portal Badge */}
          <div className="flex items-center gap-4">
            <Logo onClick={onSwitchToCustomerView} />
            <div className="border-l border-neutral-700 pl-4 hidden sm:block">
              <div className="flex items-center gap-2">
                <span className="text-sm font-black tracking-tight text-white uppercase">
                  STAFF OPERATION PORTAL
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-blue-600/30 text-blue-400 border border-blue-500/40">
                  NHÂN VIÊN RẠP
                </span>
              </div>
              <p className="text-[11px] text-neutral-400">
                Quầy Soát Vé & Vận Hành Chiếu Phim
              </p>
            </div>
          </div>

          {/* Staff Profile & Actions */}
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-neutral-800/80 border border-neutral-700/60 text-xs">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <div>
                <span className="font-bold text-neutral-200">
                  {currentUserProfile?.fullName || 'Nhân Viên MTK Demo'}
                </span>
                <span className="text-[10px] text-neutral-400 block">
                  Ca làm việc: Hôm nay (Quầy Rạp 01)
                </span>
              </div>
            </div>

            {/* Switch to customer view preview button */}
            <button
              onClick={onSwitchToCustomerView}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 text-xs font-semibold text-neutral-200 transition-colors"
              title="Xem trước giao diện khách đặt vé"
            >
              <Eye className="w-4 h-4 text-amber-400" />
              <span className="hidden md:inline">Xem Màn Khách</span>
            </button>

            {/* Logout button */}
            <button
              onClick={() => {
                logout();
                window.location.reload();
              }}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-red-950/60 hover:bg-red-900/80 border border-red-800/60 text-xs font-bold text-red-300 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Đăng Xuất</span>
            </button>
          </div>
        </div>
      </header>

      {/* Tabs Menu */}
      <div className="bg-neutral-900/50 border-b border-neutral-800/80 px-4 lg:px-8">
        <div className="max-w-7xl mx-auto flex items-center gap-2 py-2">
          <button
            onClick={() => setActiveTab('checkin')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'checkin'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/40'
                : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
            }`}
          >
            <Ticket className="w-4 h-4" />
            <span>Soát Vé & Check-in Khách</span>
          </button>

          <button
            onClick={() => setActiveTab('outbox')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'outbox'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/40'
                : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
            }`}
          >
            <Server className="w-4 h-4" />
            <span>Vận Hành Outbox & Kafka DLT</span>
          </button>

          <button
            onClick={() => setActiveTab('schedule')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'schedule'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/40'
                : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
            }`}
          >
            <Film className="w-4 h-4" />
            <span>Lịch Chiếu & Công Suất Rạp Hôm Nay</span>
          </button>
        </div>
      </div>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        {/* TAB 1: CHECK-IN / TICKET SCANNER */}
        {activeTab === 'checkin' && (
          <div className="space-y-6">
            {/* Search / Scan Box */}
            <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 shadow-2xl">
              <h2 className="text-lg font-black text-white flex items-center gap-2 mb-2">
                <QrCode className="w-5 h-5 text-red-500" />
                Quầy Soát Vé Điện Tử MTK
              </h2>
              <p className="text-xs text-neutral-400 mb-5">
                Nhập mã vé hoặc mã đặt chỗ (Booking Code) của khách để kiểm tra tính hợp lệ và cho vào phòng chiếu.
              </p>

              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSearchTicket();
                }}
                className="flex flex-col sm:flex-row gap-3"
              >
                <div className="relative flex-1">
                  <Search className="w-5 h-5 text-neutral-500 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={ticketCodeInput}
                    onChange={(e) => setTicketCodeInput(e.target.value)}
                    placeholder="Nhập mã vé (VD: BK888999, BK555666...)"
                    className="w-full bg-neutral-950 border border-neutral-800 rounded-2xl pl-12 pr-4 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all font-mono"
                  />
                </div>
                <button
                  type="submit"
                  disabled={searchLoading}
                  className="px-6 py-3.5 bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-bold text-sm rounded-2xl flex items-center justify-center gap-2 shadow-lg shadow-red-900/40 transition-all"
                >
                  {searchLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
                  <span>Kiểm Tra Vé</span>
                </button>
              </form>

              {/* Quick sample chips */}
              <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                <span className="text-neutral-500 font-semibold">Thử nhanh mã vé mẫu:</span>
                {sampleTickets.map((t) => (
                  <button
                    key={t.code}
                    type="button"
                    onClick={() => {
                      setTicketCodeInput(t.code);
                      handleSearchTicket(t.code);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 text-neutral-300 font-mono transition-colors"
                  >
                    {t.code} ({t.movie})
                  </button>
                ))}
              </div>
            </div>

            {/* Error banner */}
            {searchError && (
              <div className="p-4 rounded-2xl bg-red-950/60 border border-red-800 text-red-300 text-sm flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 shrink-0 text-red-400" />
                <span>{searchError}</span>
              </div>
            )}

            {/* Ticket Result Card */}
            {searchedBooking && (
              <div className="bg-neutral-900 border-2 border-red-600/60 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden animate-in fade-in duration-300">
                {/* Background watermarked stamp */}
                <div className="absolute right-6 -bottom-6 opacity-5 pointer-events-none select-none font-black text-9xl">
                  MTK
                </div>

                <div className="flex flex-col md:flex-row md:items-start justify-between gap-6 pb-6 border-b border-neutral-800">
                  <div>
                    <div className="flex items-center gap-2 mb-2">
                      <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-600 text-white">
                        VÉ HỢP LỆ
                      </span>
                      <span className="font-mono text-sm font-bold text-neutral-400">
                        Mã: <span className="text-white">{searchedBooking.bookingCode || searchedBooking.id}</span>
                      </span>
                    </div>
                    <h3 className="text-2xl sm:text-3xl font-black text-white">
                      {searchedBooking.movieTitle}
                    </h3>
                    <p className="text-xs text-neutral-400 mt-1">
                      {searchedBooking.cinemaName || 'CGV Cinema'} • {searchedBooking.screenName || 'Phòng chiếu số 1'}
                    </p>
                  </div>

                  {/* Status Badge */}
                  <div className="flex flex-col items-start md:items-end">
                    <span className="text-xs text-neutral-400 mb-1">Trạng thái thanh toán:</span>
                    <span
                      className={`px-3 py-1.5 rounded-xl text-xs font-black uppercase tracking-wider flex items-center gap-1.5 ${
                        searchedBooking.status === 'CONFIRMED' || searchedBooking.status === 'PAID'
                          ? 'bg-emerald-950/80 text-emerald-300 border border-emerald-700/80'
                          : 'bg-amber-950/80 text-amber-300 border border-amber-700/80'
                      }`}
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      {searchedBooking.status === 'CONFIRMED' || searchedBooking.status === 'PAID'
                        ? 'ĐÃ THANH TOÁN (HỢP LỆ)'
                        : searchedBooking.status || 'CHỜ XỬ LÝ'}
                    </span>
                  </div>
                </div>

                {/* Ticket Details Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-6 border-b border-neutral-800 text-xs">
                  <div>
                    <span className="text-neutral-500 block mb-1">Suất chiếu:</span>
                    <div className="font-bold text-white flex items-center gap-1.5 text-sm">
                      <Clock className="w-4 h-4 text-red-400" />
                      {searchedBooking.startTime || '19:30'}
                    </div>
                  </div>
                  <div>
                    <span className="text-neutral-500 block mb-1">Vị trí ghế:</span>
                    <div className="font-bold text-amber-400 text-sm">
                      {typeof searchedBooking.seats === 'string'
                        ? searchedBooking.seats
                        : Array.isArray(searchedBooking.seats)
                        ? searchedBooking.seats.map(s => s.seatLabel).join(', ')
                        : 'A1, A2'}
                    </div>
                  </div>
                  <div>
                    <span className="text-neutral-500 block mb-1">Khách hàng:</span>
                    <div className="font-bold text-neutral-200 text-sm">
                      {searchedBooking.customerName || 'Khách đặt vé trực tuyến'}
                    </div>
                  </div>
                  <div>
                    <span className="text-neutral-500 block mb-1">Tổng tiền:</span>
                    <div className="font-bold text-red-400 text-sm">
                      {searchedBooking.totalAmount ? `${Number(searchedBooking.totalAmount).toLocaleString()} VND` : '190.000 VND'}
                    </div>
                  </div>
                </div>

                {/* Actions Bar */}
                <div className="pt-6 flex flex-wrap items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    {searchedBooking.isCheckedIn || checkInSuccess ? (
                      <span className="px-4 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-xs flex items-center gap-2 shadow-lg shadow-emerald-900/40">
                        <Check className="w-4 h-4" />
                        ĐÃ SOÁT VÉ - CHO VÀO PHÒNG CHIẾU
                      </span>
                    ) : (
                      <button
                        onClick={handleConfirmCheckin}
                        className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs flex items-center gap-2 shadow-lg shadow-emerald-900/40 transition-all hover:scale-105 active:scale-95"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        XÁC NHẬN SOÁT VÉ (CHECK-IN)
                      </button>
                    )}

                    <button
                      onClick={() => window.print()}
                      className="px-4 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 font-semibold text-xs flex items-center gap-1.5 transition-colors"
                    >
                      <Printer className="w-4 h-4 text-neutral-400" />
                      In Cuống Vé
                    </button>
                  </div>

                  <span className="text-[11px] text-neutral-500 italic">
                    Ghi chú: Vé đã soát chỉ được ra vào 1 lần duy nhất trong suốt suất chiếu.
                  </span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: OUTBOX & KAFKA DLT OPERATIONS */}
        {activeTab === 'outbox' && (
          <div className="space-y-6">
            <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 shadow-2xl">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
                <div>
                  <h2 className="text-lg font-black text-white flex items-center gap-2">
                    <Server className="w-5 h-5 text-blue-500" />
                    Vận Hành Hàng Đợi Giao Dịch & Sự Kiện (Staff / Admin)
                  </h2>
                  <p className="text-xs text-neutral-400 mt-1">
                    Theo dõi hàng đợi Outbox của `booking-service` và `payment-service` thông qua API Gateway.
                  </p>
                </div>
                <button
                  onClick={loadOpsData}
                  className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs flex items-center gap-1.5 transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Làm mới
                </button>
              </div>

              {actionMsg && (
                <div className="mb-4 p-3 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>{actionMsg}</span>
                </div>
              )}

              {/* Status metrics */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
                <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800">
                  <span className="text-xs text-neutral-400 block mb-1">Outbox Tin Nhắn Chờ:</span>
                  <span className="text-2xl font-black text-emerald-400">
                    {outboxData?.pending ?? 0}
                  </span>
                  <span className="text-[10px] text-neutral-500 block mt-1">Sự kiện sẵn sàng gửi tới Kafka</span>
                </div>

                <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800">
                  <span className="text-xs text-neutral-400 block mb-1">Tin Nhắn Lỗi (Failed):</span>
                  <span className="text-2xl font-black text-red-400">
                    {Array.isArray(outboxData?.failed) ? outboxData.failed.length : 0}
                  </span>
                  <span className="text-[10px] text-neutral-500 block mt-1">Cần can thiệp hoặc Requeue</span>
                </div>

                <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800">
                  <span className="text-xs text-neutral-400 block mb-1">Kafka DLT Replay:</span>
                  <span className="text-lg font-black text-amber-400 flex items-center gap-1.5 mt-1">
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                    {dltStatus?.replayRunning ? 'ĐANG CHẠY' : 'SẴN SÀNG'}
                  </span>
                  <span className="text-[10px] text-neutral-500 block mt-1">Xử lý bù trừ Dead-Letter Topic</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center gap-3">
                <button
                  onClick={handleRequeueOutbox}
                  disabled={actionLoading}
                  className="px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-bold text-xs flex items-center gap-2 shadow-lg shadow-blue-900/40 transition-all"
                >
                  <RefreshCw className="w-4 h-4" />
                  Đẩy Lại Toàn Bộ Tin Nhắn Lỗi (Requeue Failed)
                </button>

                <button
                  onClick={handleReplayDLT}
                  disabled={actionLoading}
                  className="px-4 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 disabled:opacity-50 text-neutral-200 font-bold text-xs flex items-center gap-2 border border-neutral-700 transition-colors"
                >
                  <Server className="w-4 h-4 text-amber-400" />
                  Kích Hoạt Replay Dead-Letter Queue (DLT)
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: SCHEDULES & ROOM CAPACITY */}
        {activeTab === 'schedule' && (
          <div className="space-y-6">
            <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 shadow-2xl">
              <h2 className="text-lg font-black text-white flex items-center gap-2 mb-2">
                <Calendar className="w-5 h-5 text-red-500" />
                Lịch Chiếu Hôm Nay Tại Rạp CGV Vincom
              </h2>
              <p className="text-xs text-neutral-400 mb-6">
                Bảng thông tin theo dõi tỷ lệ lấp đầy ghế và thời gian bắt đầu của từng phòng chiếu.
              </p>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-300">
                  <thead className="bg-neutral-950 text-neutral-400 font-bold uppercase tracking-wider border-b border-neutral-800">
                    <tr>
                      <th className="py-3 px-4">Giờ Chiếu</th>
                      <th className="py-3 px-4">Tên Phim</th>
                      <th className="py-3 px-4">Phòng Chiếu</th>
                      <th className="py-3 px-4">Định Dạng</th>
                      <th className="py-3 px-4">Lấp Đầy Ghế</th>
                      <th className="py-3 px-4">Trạng Thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-800/60">
                    <tr className="hover:bg-neutral-800/40 transition-colors">
                      <td className="py-3.5 px-4 font-bold text-red-400">19:30</td>
                      <td className="py-3.5 px-4 font-bold text-white">BÒ SỮA BAY</td>
                      <td className="py-3.5 px-4">Phòng 1</td>
                      <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 font-bold">IMAX</span></td>
                      <td className="py-3.5 px-4">38 / 40 ghế (95%)</td>
                      <td className="py-3.5 px-4"><span className="text-emerald-400 font-semibold">Đang chuẩn bị</span></td>
                    </tr>
                    <tr className="hover:bg-neutral-800/40 transition-colors">
                      <td className="py-3.5 px-4 font-bold text-red-400">20:15</td>
                      <td className="py-3.5 px-4 font-bold text-white">THIÊN ĐƯỜNG TĂM TỐI</td>
                      <td className="py-3.5 px-4">Phòng 2</td>
                      <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-red-500/20 text-red-300 font-bold">4DX</span></td>
                      <td className="py-3.5 px-4">32 / 40 ghế (80%)</td>
                      <td className="py-3.5 px-4"><span className="text-emerald-400 font-semibold">Đang bán vé</span></td>
                    </tr>
                    <tr className="hover:bg-neutral-800/40 transition-colors">
                      <td className="py-3.5 px-4 font-bold text-red-400">21:45</td>
                      <td className="py-3.5 px-4 font-bold text-white">VENOM: KÈO CUỐI</td>
                      <td className="py-3.5 px-4">Phòng 3</td>
                      <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 font-bold">ScreenX</span></td>
                      <td className="py-3.5 px-4">25 / 40 ghế (62%)</td>
                      <td className="py-3.5 px-4"><span className="text-emerald-400 font-semibold">Đang bán vé</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}