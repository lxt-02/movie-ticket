import React, { useState, useEffect } from 'react';
import { MapPin, Phone, Film, Tv, Clock, CheckCircle2, Ticket } from 'lucide-react';
import { getCinemas } from '../api';
import { Link } from 'react-router-dom';

export default function CinemasPage() {
  const [cinemas, setCinemas] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    getCinemas()
      .then((data) => setCinemas(data || []))
      .finally(() => setLoading(false));
  }, []);

  const sampleCinemas = [
    {
      id: '30000000-0000-0000-0000-000000000001',
      name: 'MTK Landmark 81',
      city: 'Hồ Chí Minh',
      address: 'Tầng B1, TTTM Vincom Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM',
      phone: '1900 6017',
      screensCount: 8,
      formats: ['IMAX Laser', 'ScreenX', 'SweetBox', 'Gold Class'],
      image: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '30000000-0000-0000-0000-000000000002',
      name: 'MTK Vincom Đồng Khởi',
      city: 'Hồ Chí Minh',
      address: 'Tầng 3, 72 Lê Thánh Tôn & 45A Lý Tự Trọng, Bến Nghé, Quận 1, TP.HCM',
      phone: '1900 6017',
      screensCount: 7,
      formats: ['Dolby Atmos', '4DX', 'SweetBox'],
      image: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '30000000-0000-0000-0000-000000000003',
      name: 'MTK Royal City',
      city: 'Hà Nội',
      address: 'Tầng B2, TTTM Vincom Mega Mall Royal City, 72A Nguyễn Trãi, Thượng Đình, Thanh Xuân, Hà Nội',
      phone: '1900 6017',
      screensCount: 10,
      formats: ['IMAX Laser', 'SweetBox', 'Cine & Foret'],
      image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '30000000-0000-0000-0000-000000000004',
      name: 'MTK Bà Triệu',
      city: 'Hà Nội',
      address: 'Tầng 6, Vincom Center Bà Triệu, 191 Bà Triệu, Lê Đại Hành, Hai Bà Trưng, Hà Nội',
      phone: '1900 6017',
      screensCount: 6,
      formats: ['Laser Digital', 'Dolby Atmos'],
      image: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80',
    },
  ];

  const displayList = cinemas.length > 0 ? cinemas : sampleCinemas;

  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-10 animate-in fade-in duration-300 space-y-10">
      {/* Header */}
      <div className="bg-gradient-to-r from-red-600 to-rose-700 rounded-3xl p-8 sm:p-12 text-white shadow-xl">
        <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-white/20 inline-block mb-3">
          HỆ THỐNG CỤM RẠP TOÀN QUỐC
        </span>
        <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight">
          CỤM RẠP MTK CINEMAS
        </h1>
        <p className="text-white/80 text-sm mt-3 max-w-2xl leading-relaxed">
          Không gian sảnh chờ sang trọng, hệ thống phòng chiếu trang bị máy chiếu Laser kép và âm thanh đa chiều chuẩn Hollywood.
        </p>
      </div>

      {/* Cinemas Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {displayList.map((c, idx) => {
          const sample = sampleCinemas[idx % sampleCinemas.length];
          return (
            <div
              key={c.id || idx}
              className="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-md hover:shadow-xl transition-all duration-300 flex flex-col justify-between"
            >
              <div className="relative aspect-[16/9] overflow-hidden bg-slate-100">
                <img
                  src={c.image || sample.image}
                  alt={c.name}
                  className="w-full h-full object-cover"
                />
                <span className="absolute top-4 left-4 px-3 py-1 rounded-xl bg-slate-900/80 backdrop-blur-md text-white font-bold text-xs">
                  {c.city || sample.city}
                </span>
                <span className="absolute top-4 right-4 px-3 py-1 rounded-xl bg-red-600 text-white font-bold text-xs shadow">
                  {c.screensCount || sample.screensCount} Phòng Chiếu
                </span>
              </div>

              <div className="p-6 space-y-4">
                <div>
                  <h3 className="text-xl font-black text-slate-900 uppercase tracking-tight">
                    {c.name}
                  </h3>
                  <p className="text-xs text-slate-500 flex items-start gap-1.5 mt-1.5">
                    <MapPin className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                    <span>{c.address || sample.address}</span>
                  </p>
                  <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-1">
                    <Phone className="w-4 h-4 text-red-600 shrink-0" />
                    <span>Hotline: {c.phone || sample.phone}</span>
                  </p>
                </div>

                {/* Formats */}
                <div className="flex flex-wrap gap-1.5 pt-2 border-t border-slate-100">
                  {(c.formats || sample.formats).map((fmt, i) => (
                    <span
                      key={i}
                      className="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 font-bold text-[10px]"
                    >
                      {fmt}
                    </span>
                  ))}
                </div>

                <div className="pt-2">
                  <Link
                    to="/movies"
                    className="w-full py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-md shadow-red-600/30 transition-all"
                  >
                    <Ticket className="w-4 h-4" />
                    <span>Xem Lịch Chiếu & Đặt Vé Tại Rạp Này</span>
                  </Link>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}