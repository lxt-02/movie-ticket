import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import HeroBanner from '../components/HeroBanner';
import {
  Film,
  Ticket,
  Clock,
  Sparkles,
  ChevronRight,
  ShieldCheck,
  Building2,
  Tv,
  Award,
  Zap,
  ArrowRight
} from 'lucide-react';
import { isAuthenticated } from '../api';

export default function HomePage({ movies = [], loading = false }) {
  const navigate = useNavigate();

  const handleMovieAction = (movie) => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    navigate(`/movies/${movie.id}`);
  };

  const getAgeRatingBadge = (rating) => {
    switch (rating) {
      case 'T18':
      case 'C18':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-red-600 text-white shadow-xs">T18</span>;
      case 'T16':
      case 'C16':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-amber-600 text-white shadow-xs">T16</span>;
      case 'T13':
      case 'C13':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-yellow-500 text-slate-900 shadow-xs">T13</span>;
      case 'P':
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-emerald-600 text-white shadow-xs">P</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-16">
      {/* 1. Hero Banner Carousel */}
      <HeroBanner
        movies={movies}
        onBookNow={(movie) => handleMovieAction(movie)}
        onMovieDetail={(movie) => handleMovieAction(movie)}
        onNavigate={(path) => navigate(path)}
        onOpenRegister={() => navigate('/register')}
      />

      {/* 2. Quick Feature Bar (Placed completely below the banner with generous breathing room) */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 sm:mt-10 relative z-10">
        <div className="bg-white rounded-2xl shadow-lg border border-slate-200/80 p-4 sm:p-6 grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
          <Link
            to="/movies"
            className="flex items-center gap-3.5 group p-2 rounded-xl hover:bg-red-50/50 transition-colors"
          >
            <div className="w-12 h-12 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 group-hover:scale-105 transition-transform shadow-xs">
              <Film className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-black text-slate-800 group-hover:text-red-600 transition-colors">
                Phim Đang Chiếu
              </h4>
              <p className="text-xs text-slate-500">Đặt vé nhanh trong 1 phút</p>
            </div>
          </Link>

          <Link
            to="/cinemas"
            className="flex items-center gap-3.5 group p-2 rounded-xl hover:bg-red-50/50 transition-colors"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 group-hover:scale-105 transition-transform shadow-xs">
              <Building2 className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-black text-slate-800 group-hover:text-blue-600 transition-colors">
                Hệ Thống Cụm Rạp
              </h4>
              <p className="text-xs text-slate-500">Toàn quốc & Lịch chiếu</p>
            </div>
          </Link>

          <Link
            to="/features"
            className="flex items-center gap-3.5 group p-2 rounded-xl hover:bg-red-50/50 transition-colors"
          >
            <div className="w-12 h-12 rounded-xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600 group-hover:scale-105 transition-transform shadow-xs">
              <Sparkles className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-black text-slate-800 group-hover:text-amber-600 transition-colors">
                Công Nghệ Đỉnh Cao
              </h4>
              <p className="text-xs text-slate-500">IMAX • 4DX • Dolby Atmos</p>
            </div>
          </Link>

          <Link
            to="/my-tickets"
            className="flex items-center gap-3.5 group p-2 rounded-xl hover:bg-red-50/50 transition-colors"
          >
            <div className="w-12 h-12 rounded-xl bg-purple-50 border border-purple-100 flex items-center justify-center text-purple-600 group-hover:scale-105 transition-transform shadow-xs">
              <Ticket className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-sm font-black text-slate-800 group-hover:text-purple-600 transition-colors">
                Vé Điện Tử
              </h4>
              <p className="text-xs text-slate-500">Vào rạp quét mã QR trực tiếp</p>
            </div>
          </Link>
        </div>
      </div>

      {/* 3. Main Movies Selection Grid */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 pt-12 sm:pt-16">
        <div className="flex items-end justify-between mb-8 pb-4 border-b border-slate-200">
          <div>
            <div className="flex items-center gap-2 text-red-600 font-bold text-xs uppercase tracking-widest mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              Lịch Chiếu Phim Hot Nhất
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              PHIM ĐANG CHIẾU TẠI RẠP MTK
            </h2>
          </div>

          <Link
            to="/movies"
            className="text-xs sm:text-sm font-bold text-red-600 hover:text-red-700 flex items-center gap-1 group pb-1"
          >
            Xem tất cả <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-6">
            {[1, 2, 3, 4].map((n) => (
              <div key={n} className="bg-white rounded-2xl border border-slate-200 p-3 animate-pulse">
                <div className="w-full aspect-[2/3] bg-slate-200 rounded-xl mb-3" />
                <div className="h-4 bg-slate-200 rounded w-3/4 mb-2" />
                <div className="h-3 bg-slate-200 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {movies.map((movie) => (
              <div
                key={movie.id}
                className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs hover:shadow-xl transition-all duration-300 flex flex-col group hover:-translate-y-1"
              >
                {/* Poster Container */}
                <div
                  className="relative aspect-[2/3] overflow-hidden bg-slate-900 cursor-pointer"
                  onClick={() => handleMovieAction(movie)}
                >
                  <img
                    src={movie.posterUrl}
                    alt={movie.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    loading="lazy"
                  />
                  <div className="absolute top-2.5 left-2.5 z-10 flex flex-col gap-1">
                    {getAgeRatingBadge(movie.ageRating)}
                  </div>

                  <div className="absolute top-2.5 right-2.5 z-10">
                    <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-slate-900/80 text-white backdrop-blur-xs border border-white/20">
                      {movie.format || '2D Digital'}
                    </span>
                  </div>

                  {/* Hover Overlay with Action Buttons */}
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-950/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4">
                    <p className="text-xs text-slate-300 line-clamp-3 mb-3">{movie.synopsis}</p>
                    <div className="flex gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMovieAction(movie);
                        }}
                        className="flex-1 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs text-center transition-colors shadow-sm flex items-center justify-center gap-1.5"
                      >
                        <Ticket className="w-3.5 h-3.5" />
                        Mua Vé
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMovieAction(movie);
                        }}
                        className="px-3 py-2 rounded-xl bg-white/20 hover:bg-white/30 text-white font-bold text-xs backdrop-blur-xs transition-colors flex items-center justify-center"
                      >
                        Chi Tiết
                      </button>
                    </div>
                  </div>
                </div>

                {/* Movie Meta */}
                <div className="p-3.5 sm:p-4 flex-1 flex flex-col justify-between">
                  <div>
                    <h3
                      onClick={() => handleMovieAction(movie)}
                      className="font-black text-sm sm:text-base text-slate-900 group-hover:text-red-600 transition-colors line-clamp-1 mb-1 cursor-pointer"
                      title={movie.title}
                    >
                      {movie.title}
                    </h3>
                    <p className="text-xs text-slate-500 font-medium line-clamp-1 mb-2">
                      {Array.isArray(movie.genres) ? movie.genres.join(', ') : movie.genres}
                    </p>
                  </div>

                  <div className="pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {movie.durationMinutes} phút
                    </span>
                    <button
                      onClick={() => handleMovieAction(movie)}
                      className="font-bold text-red-600 hover:underline flex items-center gap-0.5"
                    >
                      Đặt ngay
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 4. Experience Highlights Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 pt-16">
        <div className="bg-gradient-to-r from-red-600 to-rose-700 rounded-3xl p-6 sm:p-10 text-white shadow-xl relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <span className="px-3 py-1 rounded-full bg-white/20 text-white text-xs font-bold tracking-wider uppercase mb-3 inline-block">
              MTK Special Experience
            </span>
            <h3 className="text-2xl sm:text-4xl font-black mb-3">
              CHUẨN MỰC ĐIỆN ẢNH VƯỢT TRỘI
            </h3>
            <p className="text-red-100 text-sm sm:text-base mb-6 leading-relaxed">
              Trang bị hệ thống chiếu Laser 4K thế hệ mới, âm thanh vòm Dolby Atmos đa chiều cùng hệ thống ghế đôi Sweetbox bọc da êm ái hàng đầu Việt Nam.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link
                to="/features"
                className="px-5 py-2.5 rounded-xl bg-white text-red-700 font-bold text-sm hover:bg-red-50 transition-colors shadow-sm flex items-center gap-1.5"
              >
                Khám phá công nghệ
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/cinemas"
                className="px-5 py-2.5 rounded-xl bg-red-800/60 hover:bg-red-800 text-white font-bold text-sm transition-colors border border-white/20"
              >
                Danh sách rạp MTK
              </Link>
            </div>
          </div>

          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-10 translate-y-10">
            <Film className="w-96 h-96" />
          </div>
        </div>
      </section>
    </div>
  );
}
