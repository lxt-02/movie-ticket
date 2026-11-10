import React from 'react';
import { Link } from 'react-router-dom';
import Logo from './Logo';
import {
  Sparkles,
  Ticket,
  ShieldCheck,
  Phone,
  Mail,
  MapPin,
  ChevronRight,
  Tv,
  Film,
  Building2
} from 'lucide-react';

export default function MtkFooter() {
  const formats = [
    { name: 'IMAX', desc: 'Laser Cực Đại' },
    { name: '4DX', desc: 'Đa Giác Quan' },
    { name: 'SCREENX', desc: 'Góc Rộng 270°' },
    { name: 'GOLD CLASS', desc: 'VIP Thượng Hạng' },
    { name: 'SWEETBOX', desc: 'Ghế Đôi Riêng Tư' },
    { name: 'DOLBY ATMOS', desc: 'Âm Thanh Vòm 360°' },
    { name: 'LASER 4K', desc: 'Độ Nét Siêu Thực' },
    { name: 'CINE & FORET', desc: 'Không Gian Xanh' },
  ];

  return (
    <div className="w-full mt-16">
      {/* 1. CINEMA FORMATS SHOWCASE STRIP */}
      <div className="border-t border-b border-slate-200 bg-white py-6 px-4 sm:px-6">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-4 gap-2">
            <span className="text-xs font-black tracking-widest text-slate-700 uppercase flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-red-600" />
              CÔNG NGHỆ PHÒNG CHIẾU ĐẶC BIỆT TẠI MTK CINEMAS
            </span>
            <Link
              to="/features"
              className="text-xs text-red-600 font-bold hover:underline flex items-center gap-1"
            >
              <span>Xem chi tiết công nghệ</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
            {formats.map((fmt, i) => (
              <Link
                key={i}
                to="/features"
                className="group p-3 rounded-2xl bg-slate-50 hover:bg-red-50 border border-slate-200 hover:border-red-300 text-center transition-all hover:-translate-y-1 shadow-xs"
              >
                <div className="font-black text-xs text-slate-800 group-hover:text-red-600 tracking-wider">
                  {fmt.name}
                </div>
                <div className="text-[10px] text-slate-500 line-clamp-1 mt-0.5 font-medium">
                  {fmt.desc}
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* 2. MAIN FOOTER */}
      <footer className="bg-slate-900 text-slate-300 pt-14 pb-10 px-4 sm:px-6 lg:px-8 text-xs">
        <div className="max-w-7xl mx-auto">
          {/* Top 4 Columns */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-8 pb-10 border-b border-slate-800">
            {/* Col 1: Rạp MTK */}
            <div>
              <h5 className="font-black text-white uppercase tracking-wider text-sm mb-4 flex items-center gap-2">
                <Film className="w-4 h-4 text-red-500" />
                Rạp MTK Cinemas
              </h5>
              <ul className="space-y-2.5 text-xs text-slate-400 font-medium">
                <li>
                  <Link to="/about" className="hover:text-white transition-colors">
                    Giới thiệu MTK Cinemas
                  </Link>
                </li>
                <li>
                  <Link to="/features" className="hover:text-white transition-colors">
                    Đặc trưng & Công nghệ rạp
                  </Link>
                </li>
                <li>
                  <Link to="/movies" className="hover:text-white transition-colors">
                    Phim đang chiếu tại rạp
                  </Link>
                </li>
                <li>
                  <Link to="/cinemas" className="hover:text-white transition-colors">
                    Hệ thống rạp toàn quốc
                  </Link>
                </li>
                <li>
                  <Link to="/my-tickets" className="hover:text-white transition-colors">
                    Tra cứu vé điện tử đã đặt
                  </Link>
                </li>
              </ul>
            </div>

            {/* Col 2: Điều Khoản Sử Dụng */}
            <div>
              <h5 className="font-black text-white uppercase tracking-wider text-sm mb-4 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-red-500" />
                Điều Khoản & Dịch Vụ
              </h5>
              <ul className="space-y-2.5 text-xs text-slate-400 font-medium">
                <li>
                  <Link to="/terms" className="hover:text-white transition-colors">
                    Điều khoản chung của rạp
                  </Link>
                </li>
                <li>
                  <Link to="/terms" className="hover:text-white transition-colors">
                    Điều khoản giao dịch vé trực tuyến
                  </Link>
                </li>
                <li>
                  <Link to="/terms" className="hover:text-white transition-colors">
                    Chính sách thanh toán PayOS & Hoàn vé
                  </Link>
                </li>
                <li>
                  <Link to="/terms" className="hover:text-white transition-colors">
                    Chính sách bảo mật thông tin
                  </Link>
                </li>
                <li>
                  <Link to="/terms" className="hover:text-white transition-colors">
                    Câu hỏi thường gặp (FAQ)
                  </Link>
                </li>
              </ul>
            </div>

            {/* Col 3: Kết Nối Với Chúng Tôi */}
            <div>
              <h5 className="font-black text-white uppercase tracking-wider text-sm mb-4">
                Chăm Sóc Khách Hàng
              </h5>
              <div className="space-y-3 text-xs text-slate-400">
                <p className="flex items-center gap-2">
                  <Phone className="w-4 h-4 text-red-500 shrink-0" />
                  <span>Hotline: <strong className="text-white text-sm font-mono">1900 6017</strong></span>
                </p>
                <p className="flex items-center gap-2">
                  <Mail className="w-4 h-4 text-red-500 shrink-0" />
                  <span>Email: <strong className="text-white">support@mtkcinemas.vn</strong></span>
                </p>
                <p className="text-[11px] leading-relaxed text-slate-400">
                  Giờ làm việc: 8:00 - 22:00 (Tất cả các ngày bao gồm Lễ, Tết).
                </p>
                <div className="pt-2">
                  <Link
                    to="/my-tickets"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-600/20 hover:bg-red-600/30 border border-red-500/40 text-red-400 text-xs font-bold transition-colors"
                  >
                    <Ticket className="w-3.5 h-3.5" />
                    Quét Mã Check-in Vé
                  </Link>
                </div>
              </div>
            </div>

            {/* Col 4: Logo & Bản Quyền */}
            <div className="space-y-3">
              <Logo size="md" />
              <p className="text-[11px] text-slate-400 leading-relaxed mt-2">
                <strong>CÔNG TY CỔ PHẦN ĐIỆN ẢNH MTK VIỆT NAM</strong>
                <br />
                Đơn vị tiên phong vận hành hệ sinh thái rạp chiếu phim kỹ thuật số tiêu chuẩn Hollywood.
              </p>
              <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800">
                Chứng nhận rạp chiếu phim chuẩn ISO 9001:2015 & Hệ thống đặt vé bảo mật đa tầng.
              </div>
            </div>
          </div>

          {/* Bottom Copyright Strip */}
          <div className="pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
            <p>© {new Date().getFullYear()} MTK Cinemas Vietnam. All rights reserved.</p>
            <div className="flex items-center gap-4">
              <Link to="/about" className="hover:text-slate-300 transition-colors">Giới thiệu</Link>
              <Link to="/cinemas" className="hover:text-slate-300 transition-colors">Cụm rạp</Link>
              <Link to="/terms" className="hover:text-slate-300 transition-colors">Chính sách</Link>
              <Link to="/features" className="hover:text-slate-300 transition-colors">Công nghệ</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
