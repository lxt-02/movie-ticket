import React, { useState } from 'react';
import { Clock, Ticket, Star, Sparkles, Info, Calendar } from 'lucide-react';

export default function MovieList({
  movies = [],
  onSelectMovie,
  onViewDetail,
  isAuthenticated = false,
  onRequireAuth
}) {
  const [tab, setTab] = useState('NOW_SHOWING'); // 'NOW_SHOWING' | 'COMING_SOON'

  const handleAction = (actionType, movie) => {
    if (!isAuthenticated) {
      if (onRequireAuth) onRequireAuth();
      return;
    }
    if (actionType === 'BOOK') {
      onSelectMovie(movie);
    } else {
      onViewDetail(movie);
    }
  };

  const getAgeRatingBadge = (rating) => {
    switch (rating) {
      case 'T18':
      case 'C18':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-red-600 text-white shadow">C18</span>;
      case 'T16':
      case 'C16':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-amber-600 text-white shadow">C16</span>;
      case 'T13':
      case 'C13':
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-yellow-500 text-neutral-950 shadow">C13</span>;
      case 'P':
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-black bg-emerald-600 text-white shadow">P</span>;
    }
  };

  return (
    <div id="movies-section" className="w-full">
      {/* 1. CGV RETRO HEADER: ── MOVIE SELECTION ── */}
      <div className="text-center mb-8 relative">
        <div className="flex items-center justify-center gap-4">
          <div className="h-[2px] w-12 sm:w-28 bg-gradient-to-r from-transparent to-red-600" />
          <h2 className="text-2xl sm:text-3xl font-black text-white tracking-widest uppercase font-serif">
            MOVIE SELECTION
          </h2>
          <div className="h-[2px] w-12 sm:w-28 bg-gradient-to-l from-transparent to-red-600" />
        </div>
        <p className="text-xs text-neutral-400 mt-1 uppercase tracking-widest">
          Danh Sách Phim Tuyển Chọn Tại Rạp MTK Cinemas
        </p>

        {/* Sub-tabs: Now Showing / Coming Soon */}
        <div className="mt-6 inline-flex p-1 bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg">
          <button
            onClick={() => setTab('NOW_SHOWING')}
            className={`px-5 py-2 rounded-xl text-xs font-black uppercase tracking-wider transition-all ${
              tab === 'NOW_SHOWING'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/50'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Phim Đang Chiếu
          </button>
          <button
            onClick={() => setTab('COMING_SOON')}
            className={`px-5 py-2 rounded-xl text-xs font-black uppercase tracking-wider transition-all ${
              tab === 'COMING_SOON'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/50'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Phim Sắp Chiếu
          </button>
        </div>
      </div>

      {/* 2. MOVIE POSTERS GRID */}
      {movies.length === 0 ? (
        <div className="text-center py-20 bg-neutral-900/50 rounded-3xl border border-neutral-800">
          <Sparkles className="w-10 h-10 text-neutral-500 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-neutral-300">Không tìm thấy phim phù hợp</h3>
          <p className="text-xs text-neutral-500 mt-1">Vui lòng thử tìm kiếm lại với tên phim khác!</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {movies.map((movie) => (
            <div
              key={movie.id}
              className="group bg-neutral-900 border border-neutral-800 hover:border-red-600/70 rounded-3xl overflow-hidden shadow-xl hover:shadow-2xl hover:shadow-red-950/30 transition-all duration-300 flex flex-col justify-between"
            >
              {/* Poster Image Container */}
              <div className="relative aspect-[3/4] overflow-hidden bg-neutral-950 cursor-pointer" onClick={() => handleAction('DETAIL', movie)}>
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
                  onError={(e) => {
                    e.target.src = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80';
                  }}
                />

                {/* Gradient shade */}
                <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-transparent to-transparent opacity-80" />

                {/* Top Badges */}
                <div className="absolute top-3 left-3 flex items-center gap-1.5">
                  {getAgeRatingBadge(movie.ageRating)}
                  {movie.format && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-black bg-neutral-900/90 text-amber-300 border border-amber-500/30 shadow backdrop-blur-sm">
                      {movie.format.split('•')[0]}
                    </span>
                  )}
                </div>

                {/* Rating & Duration */}
                <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between text-xs text-neutral-300">
                  <span className="flex items-center gap-1 font-bold text-amber-400 bg-neutral-950/80 px-2 py-0.5 rounded-lg border border-neutral-800">
                    <Star className="w-3.5 h-3.5 fill-amber-400" /> 4.9
                  </span>
                  <span className="flex items-center gap-1 font-medium bg-neutral-950/80 px-2 py-0.5 rounded-lg border border-neutral-800">
                    <Clock className="w-3.5 h-3.5 text-red-400" />
                    {movie.durationMinutes}p
                  </span>
                </div>
              </div>

              {/* Movie Info */}
              <div className="p-4 flex-1 flex flex-col justify-between">
                <div>
                  <h3
                    onClick={() => handleAction('DETAIL', movie)}
                    className="font-black text-base text-white group-hover:text-red-400 transition-colors uppercase tracking-tight line-clamp-1 mb-1 cursor-pointer"
                  >
                    {movie.title}
                  </h3>

                  {/* Genres */}
                  <div className="flex flex-wrap gap-1 mb-2">
                    {(Array.isArray(movie.genres) ? movie.genres : []).slice(0, 2).map((g, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-0.5 rounded text-[10px] font-semibold bg-neutral-800 text-neutral-300"
                      >
                        {typeof g === 'object' ? g.name : g}
                      </span>
                    ))}
                  </div>

                  <p className="text-[11px] text-neutral-400 line-clamp-2 leading-relaxed mb-4">
                    {movie.synopsis || 'Phim đang khởi chiếu tại các rạp MTK trên toàn quốc.'}
                  </p>
                </div>

                {/* Action Buttons: 2 buttons like CGV */}
                <div className="pt-3 border-t border-neutral-800/80 grid grid-cols-2 gap-2">
                  <button
                    onClick={() => handleAction('DETAIL', movie)}
                    className="py-2 px-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs flex items-center justify-center gap-1 border border-neutral-700 transition-colors"
                  >
                    <Info className="w-3.5 h-3.5 text-neutral-400" />
                    <span>Chi Tiết</span>
                  </button>

                  <button
                    onClick={() => handleAction('BOOK', movie)}
                    className="py-2 px-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase tracking-wider flex items-center justify-center gap-1 shadow-md shadow-red-950 hover:scale-105 active:scale-95 transition-all"
                  >
                    <Ticket className="w-3.5 h-3.5 fill-white" />
                    <span>Mua Vé</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}