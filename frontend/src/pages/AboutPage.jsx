import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Award,
  Sparkles,
  Ticket,
  ShieldCheck,
  MapPin,
  Users,
  HeartHandshake,
  Film,
  Building2,
  ChevronRight,
  ArrowRight
} from 'lucide-react';
import { isAuthenticated } from '../api';

export default function AboutPage() {
  const navigate = useNavigate();

  const cinemaLocations = [
    { name: 'MTK Landmark 81', address: 'Tầng B1, TTTM Vincom Landmark 81, 720A Điện Biên Phủ, Bình Thạnh, TP.HCM', screens: '8 Phòng (IMAX Laser & ScreenX)' },
    { name: 'MTK Vincom Đồng Khởi', address: 'Tầng 3, 72 Lê Thánh Tôn & 45A Lý Tự Trọng, Bến Nghé, Quận 1, TP.HCM', screens: '7 Phòng (Dolby Atmos & Gold Class)' },
    { name: 'MTK Royal City', address: 'Tầng B2, 72A Nguyễn Trãi, Thượng Đình, Thanh Xuân, Hà Nội', screens: '10 Phòng (SweetBox & 4DX)' },
    { name: 'MTK Bà Triệu', address: 'Tầng 6, 191 Bà Triệu, Lê Đại Hành, Hai Bà Trưng, Hà Nội', screens: '6 Phòng (Laser Digital)' },
  ];

  const handleBookNow = () => {
    if (!isAuthenticated()) {
      navigate('/login');
    } else {
      navigate('/movies');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 space-y-10">
        {/* 1. Hero Cover Banner */}
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 rounded-3xl p-8 sm:p-12 text-white shadow-xl relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-white/20 inline-block mb-3 backdrop-blur-xs">
              VỀ CHÚNG TÔI • MTK CINEMAS
            </span>
            <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight">
              HỆ THỐNG RẠP CHIẾU PHIM MTK
            </h1>
            <p className="text-red-100 text-sm sm:text-base mt-3 leading-relaxed">
              Tiên phong mang đến chuẩn mực giải trí điện ảnh quốc tế hàng đầu tại Việt Nam với công nghệ hình ảnh Laser 4K và âm thanh vòm Dolby Atmos.
            </p>
          </div>
          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-12 translate-y-12">
            <Film className="w-80 h-80" />
          </div>
        </div>

        {/* 2. Story & Mission */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-sm grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
          <div className="space-y-4 text-sm text-slate-600 leading-relaxed">
            <div className="inline-flex items-center gap-1.5 text-red-600 font-bold text-xs uppercase tracking-widest">
              <Sparkles className="w-4 h-4" />
              Sứ mệnh của chúng tôi
            </div>
            <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight">
              NÂNG TẦM TRẢI NGHIỆM ĐIỆN ẢNH VIỆT
            </h2>
            <p>
              Ra đời với khát vọng kiến tạo một chuẩn mực thưởng thức điện ảnh hoàn toàn khác biệt, <strong className="text-red-600 font-bold">MTK Cinemas</strong> không ngừng đổi mới công nghệ chiếu, mở rộng hệ thống phòng chiếu hiện đại và hoàn thiện dịch vụ chăm sóc khách hàng chuẩn 5 sao.
            </p>
            <p>
              Chúng tôi tin rằng rạp chiếu phim không đơn thuần là nơi xem phim, mà là một không gian văn hóa sống động - nơi người xem kết nối cảm xúc, chia sẻ niềm vui trọn vẹn bên bạn bè và gia đình.
            </p>
            <p>
              Hệ thống đặt vé trực tuyến của MTK được tích hợp công nghệ vi dịch vụ Microservices tân tiến, bảo đảm khách hàng luôn giữ được chỗ ngồi đẹp tức thời mà không lo nghẽn mạng vào những dịp công chiếu bom tấn.
            </p>
          </div>

          {/* Metric Stats Cards */}
          <div className="grid grid-cols-2 gap-4">
            <div className="p-5 rounded-2xl bg-red-50/70 border border-red-100 text-center space-y-1 shadow-xs">
              <span className="text-3xl font-black text-red-600">30+</span>
              <span className="text-xs text-slate-600 block font-bold">Cụm rạp toàn quốc</span>
            </div>
            <div className="p-5 rounded-2xl bg-amber-50/70 border border-amber-100 text-center space-y-1 shadow-xs">
              <span className="text-3xl font-black text-amber-600">100%</span>
              <span className="text-xs text-slate-600 block font-bold">Máy chiếu Laser 4K</span>
            </div>
            <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-100 text-center space-y-1 shadow-xs">
              <span className="text-3xl font-black text-emerald-600">5M+</span>
              <span className="text-xs text-slate-600 block font-bold">Khán giả tin chọn</span>
            </div>
            <div className="p-5 rounded-2xl bg-blue-50/70 border border-blue-100 text-center space-y-1 shadow-xs">
              <span className="text-3xl font-black text-blue-600">24/7</span>
              <span className="text-xs text-slate-600 block font-bold">Hỗ trợ trực tuyến</span>
            </div>
          </div>
        </div>

        {/* 3. Core Values */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600">
              <Award className="w-6 h-6" />
            </div>
            <h3 className="font-black text-slate-900 text-lg">Chất Lượng Đỉnh Cao</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Hệ thống máy chiếu Laser, dàn loa Dolby Atmos và màn hình cực đại luôn được cân chỉnh định kỳ theo tiêu chuẩn khắt khe từ Hollywood.
            </p>
          </div>

          <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
              <HeartHandshake className="w-6 h-6" />
            </div>
            <h3 className="font-black text-slate-900 text-lg">Tận Tâm Phục Vụ</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Đội ngũ nhân viên thân thiện, chu đáo trong từng khâu phục vụ bắp nước giòn thơm và hướng dẫn khách hàng check-in vào rạp.
            </p>
          </div>

          <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-purple-50 border border-purple-100 flex items-center justify-center text-purple-600">
              <Ticket className="w-6 h-6" />
            </div>
            <h3 className="font-black text-slate-900 text-lg">Tiện Lợi Số Hóa</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Đặt vé và chọn ghế thông minh trong 1 phút, nhận mã vé điện tử QR Code quét mã vào phòng chiếu không cần xếp hàng đổi vé giấy.
            </p>
          </div>
        </div>

        {/* 4. Featured Locations Strip */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h3 className="text-xl font-black text-slate-900 uppercase">
                CỤM RẠP NỔI BẬT TRÊN TOÀN QUỐC
              </h3>
              <p className="text-xs text-slate-500 mt-1">Các trung tâm chiếu phim trọng điểm của hệ thống MTK</p>
            </div>
            <Link
              to="/cinemas"
              className="text-xs font-bold text-red-600 hover:text-red-700 flex items-center gap-1"
            >
              Xem tất cả cụm rạp <ChevronRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {cinemaLocations.map((loc, i) => (
              <div key={i} className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-red-600 shrink-0 shadow-xs">
                  <MapPin className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-slate-900">{loc.name}</h4>
                  <p className="text-xs text-slate-500 mt-0.5">{loc.address}</p>
                  <span className="inline-block mt-2 px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-700">
                    {loc.screens}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* CTA */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            <span className="text-xs text-slate-600 font-medium">
              Bạn đã sẵn sàng tận hưởng những thước phim mãn nhãn nhất?
            </span>
            <button
              onClick={handleBookNow}
              className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-sm shadow-red-200 transition-all hover:scale-105 active:scale-95"
            >
              <Ticket className="w-4 h-4" />
              <span>Đặt Vé Ngay</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
