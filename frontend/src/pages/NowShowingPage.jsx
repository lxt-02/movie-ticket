import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Film,
  Search,
  Star,
  Clock,
  Ticket,
  Info,
  Sparkles,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';
import { isAuthenticated } from '../api';

export default function NowShowingPage({ movies = [] }) {
  const navigate = useNavigate();
  const [selectedGenre, setSelectedGenre] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const genres = ['ALL', 'Hành Động', 'Hài', 'Phiêu Lưu', 'Gia Đình', 'Tâm Lý', 'Khoa Học Viễn Tưởng'];

  const filteredMovies = movies.filter((m) => {
    const matchesGenre =
      selectedGenre === 'ALL' ||
      (Array.isArray(m.genres) &&
        m.genres.some((g) => (typeof g === 'object' ? g.name : g).toLowerCase().includes(selectedGenre.toLowerCase())));
    const matchesQuery =
      !searchQuery.trim() ||
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (m.actors && m.actors.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesGenre && matchesQuery;
  });

  const handleAction = (action, movie) => {
    if (!isAuthenticated()) {
      // Must login first to proceed
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
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6">
        {/* 1. Header Banner */}
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 rounded-3xl p-6 sm:p-8 text-white shadow-lg mb-8 relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/20 text-white text-xs font-bold uppercase tracking-wider mb-2 backdrop-blur-xs">
              <Sparkles className="w-3.5 h-3.5" />
              MTK Cinemas Now Showing
            </div>
            <h1 className="text-2xl sm:text-4xl font-black tracking-tight uppercase">
              DANH SÁCH PHIM ĐANG CHIẾU
            </h1>
            <p className="text-red-100 text-xs sm:text-sm mt-2 leading-relaxed">
              Trải nghiệm các bom tấn điện ảnh đỉnh cao chuẩn quốc tế với hình ảnh Laser 4K sắc nét và âm thanh Dolby Atmos tại hệ thống rạp MTK.
            </p>
          </div>
          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-8 translate-y-8">
            <Film className="w-64 h-64" />
          </div>
        </div>

        {/* 2. Filter & Search Controls */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-8 bg-white p-3 sm:p-4 rounded-2xl border border-slate-200/90 shadow-xs">
          {/* Genre Chips */}
          <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-2 md:pb-0 scrollbar-none">
            {genres.map((g) => (
              <button
                key={g}
                onClick={() => setSelectedGenre(g)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
                  selectedGenre === g
                    ? 'bg-red-600 text-white shadow-sm shadow-red-200'
                    : 'bg-slate-100 text-slate-600 hover:text-slate-900 hover:bg-slate-200 border border-slate-200/60'
                }`}
              >
                {g === 'ALL' ? 'Tất Cả Thể Loại' : g}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên phim, diễn viên..."
              className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-red-500 shadow-xs"
            />
          </div>
        </div>

        {/* 3. Movie Grid - Responsive, neat padding, compact cards */}
        {filteredMovies.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-3xl border border-slate-200 shadow-xs">
            <Sparkles className="w-10 h-10 text-slate-400 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-800">Không tìm thấy phim phù hợp</h3>
            <p className="text-xs text-slate-500 mt-1">Vui lòng thử chọn thể loại khác hoặc xóa từ khóa tìm kiếm.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {filteredMovies.map((movie) => (
              <div
                key={movie.id}
                className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs hover:shadow-xl transition-all duration-300 flex flex-col group hover:-translate-y-1"
              >
                {/* Poster Container */}
                <div
                  className="relative aspect-[2/3] overflow-hidden bg-slate-900 cursor-pointer"
                  onClick={() => handleAction('DETAIL', movie)}
                >
                  <img
                    src={movie.posterUrl}
                    alt={movie.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    loading="lazy"
                    onError={(e) => {
                      e.target.src = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80';
                    }}
                  />
                  <div className="absolute top-2.5 left-2.5 z-10 flex flex-col gap-1">
                    {getAgeRatingBadge(movie.ageRating)}
                  </div>

                  <div className="absolute top-2.5 right-2.5 z-10">
                    <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-slate-900/80 text-white backdrop-blur-xs border border-white/20">
                      {movie.format || '2D Digital'}
                    </span>
                  </div>

                  {/* Hover Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-950/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4">
                    <p className="text-xs text-slate-300 line-clamp-3 mb-3">{movie.synopsis}</p>
                    <div className="flex gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAction('BOOK', movie);
                        }}
                        className="flex-1 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs text-center transition-colors shadow-sm flex items-center justify-center gap-1.5"
                      >
                        <Ticket className="w-3.5 h-3.5" />
                        Mua Vé
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAction('DETAIL', movie);
                        }}
                        className="px-3 py-2 rounded-xl bg-white/20 hover:bg-white/30 text-white font-bold text-xs backdrop-blur-xs transition-colors flex items-center justify-center"
                      >
                        Chi Tiết
                      </button>
                    </div>
                  </div>
                </div>

                {/* Movie Meta Information */}
                <div className="p-3.5 sm:p-4 flex-1 flex flex-col justify-between">
                  <div>
                    <h3
                      onClick={() => handleAction('DETAIL', movie)}
                      className="font-black text-sm sm:text-base text-slate-900 group-hover:text-red-600 transition-colors line-clamp-1 mb-1 cursor-pointer"
                      title={movie.title}
                    >
                      {movie.title}
                    </h3>
                    <p className="text-xs text-slate-500 font-medium line-clamp-1 mb-2">
                      {Array.isArray(movie.genres) ? movie.genres.join(', ') : movie.genres}
                    </p>
                  </div>

                  {/* Actions & Duration */}
                  <div className="pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {movie.durationMinutes} phút
                    </span>
                    <button
                      onClick={() => handleAction('BOOK', movie)}
                      className="font-bold text-red-600 hover:text-red-700 flex items-center gap-0.5 hover:underline"
                    >
                      Đặt ngay
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
