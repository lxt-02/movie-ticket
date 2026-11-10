import React, { useState, useEffect } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Ticket,
  Star,
  Play,
  Calendar,
  Clock,
  Info,
  Sparkles
} from 'lucide-react';

export default function HeroBanner({
  movies = [],
  onBookNow,
  onMovieDetail
}) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const displayMovies = movies.length > 0 ? movies.slice(0, 5) : [
    {
      id: '20000000-0000-0000-0000-000000000010',
      title: 'BÒ SỮA BAY',
      originalTitle: 'Flying Dairy Cow',
      releaseDate: '06 11 2026',
      ageRating: 'P',
      durationMinutes: 115,
      genres: ['Hài', 'Phiêu Lưu', 'Gia Đình'],
      director: 'Nguyễn Phạm Thành Đạt',
      actors: 'Nguyễn Hùng, Ngọc Xuân, Võ Điền Gia Huy, Tiểu Vy, Huy Tít',
      bannerUrl: 'https://images.unsplash.com/photo-1500595046743-cd271d694d30?w=1400&auto=format&fit=crop&q=80',
      synopsis: 'Dự án phim điện ảnh đặc sắc được tuyển chọn APM & DANAFF. Một câu chuyện kỳ thú về chú bò sữa đặc biệt và giấc mơ bay cao của những con người nông thôn.',
    }
  ];

  const current = displayMovies[currentIndex] || displayMovies[0];

  useEffect(() => {
    if (displayMovies.length <= 1) return;
    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % displayMovies.length);
    }, 6000);
    return () => clearInterval(interval);
  }, [displayMovies.length]);

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev - 1 + displayMovies.length) % displayMovies.length);
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev + 1) % displayMovies.length);
  };

  return (
    <div className="w-full">
      {/* MTK MAIN CAROUSEL HERO */}
      <div className="relative w-full overflow-hidden shadow-xl bg-slate-950 group">
        <div className="relative w-full h-[420px] sm:h-[480px] md:h-[540px] overflow-hidden">
          {/* Main Backdrop Image */}
          <img
            src={current.bannerUrl || current.posterUrl}
            alt={current.title}
            className="w-full h-full object-cover object-center filter brightness-[0.55] transition-all duration-700 ease-out"
          />

          {/* Vignette Gradients */}
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-r from-slate-950/90 via-slate-950/40 to-transparent" />

          {/* Left / Right Carousel Controls */}
          <button
            onClick={handlePrev}
            className="absolute left-4 sm:left-8 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/40 hover:bg-red-600 text-white flex items-center justify-center transition-all z-20 backdrop-blur-md border border-white/20 hover:border-red-600 shadow-lg cursor-pointer"
            aria-label="Previous slide"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
          <button
            onClick={handleNext}
            className="absolute right-4 sm:right-8 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/40 hover:bg-red-600 text-white flex items-center justify-center transition-all z-20 backdrop-blur-md border border-white/20 hover:border-red-600 shadow-lg cursor-pointer"
            aria-label="Next slide"
          >
            <ChevronRight className="w-6 h-6" />
          </button>

          {/* Hero Content Overlay */}
          <div className="absolute inset-0 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 p-6 sm:p-12 flex flex-col justify-end z-10 pb-16">
            <div className="max-w-3xl space-y-3 animate-in fade-in duration-300">
              <div className="flex flex-wrap items-center gap-2">
                <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-600 text-white shadow-md shadow-red-950 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  PHIM NỔI BẬT MTK
                </span>
                <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-900/90 text-amber-400 border border-amber-500/30 flex items-center gap-1 backdrop-blur-sm">
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" /> 4.9/5
                </span>
                <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-900/90 text-slate-200 border border-slate-700 backdrop-blur-sm">
                  {current.ageRating || 'P'}
                </span>
                <span className="hidden sm:inline-block px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-900/90 text-red-300 border border-red-500/30 backdrop-blur-sm">
                  {current.format || 'IMAX Laser • 2D Digital'}
                </span>
              </div>

              <h1 className="text-3xl sm:text-5xl md:text-6xl font-black text-white uppercase tracking-tight drop-shadow-xl line-clamp-2">
                {current.title}
              </h1>

              {current.actors && (
                <p className="text-xs sm:text-sm text-slate-300 line-clamp-1 font-medium">
                  <strong className="text-white">Diễn viên:</strong> {current.actors}
                </p>
              )}

              <p className="text-xs sm:text-sm text-slate-300 line-clamp-2 max-w-2xl leading-relaxed">
                {current.synopsis}
              </p>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center gap-3 pt-2">
                <button
                  onClick={() => onBookNow(current)}
                  className="px-6 py-3 rounded-2xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-black text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2 shadow-lg shadow-red-950 hover:scale-105 active:scale-95 transition-all cursor-pointer"
                >
                  <Ticket className="w-4 h-4 fill-white" />
                  <span>MUA VÉ NGAY</span>
                </button>

                <button
                  onClick={() => onMovieDetail(current)}
                  className="px-5 py-3 rounded-2xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 font-bold text-xs sm:text-sm flex items-center gap-2 backdrop-blur-md transition-all hover:border-slate-500 cursor-pointer"
                >
                  <Info className="w-4 h-4 text-red-400" />
                  <span>Xem Chi Tiết</span>
                </button>
              </div>
            </div>
          </div>

          {/* Dots Indicator */}
          <div className="absolute bottom-5 left-1/2 -translate-x-1/2 flex items-center gap-2 z-20">
            {displayMovies.map((_, idx) => (
              <button
                key={idx}
                onClick={() => setCurrentIndex(idx)}
                className={`h-2 rounded-full transition-all cursor-pointer ${
                  currentIndex === idx
                    ? 'w-8 bg-red-600 shadow-md shadow-red-900'
                    : 'w-2 bg-white/40 hover:bg-white/70'
                }`}
                aria-label={`Slide ${idx + 1}`}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
