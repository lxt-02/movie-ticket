import React, { useState, useEffect } from 'react';
import { X, Clock, Calendar, Film, MapPin, Ticket, AlertCircle } from 'lucide-react';
import { getShowtimes, getCinemas } from '../api';

export default function MovieDetailModal({ movie, onClose, onSelectShowtime }) {
  const [showtimes, setShowtimes] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!movie) return;
    setLoading(true);
    Promise.all([getShowtimes(movie.id), getCinemas()])
      .then(([sts, cns]) => {
        setShowtimes(sts || []);
        setCinemas(cns || []);
        if (cns && cns.length > 0) setSelectedCinema(cns[0].id);
      })
      .finally(() => setLoading(false));
  }, [movie]);

  if (!movie) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden max-h-[90vh] flex flex-col">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-20 w-9 h-9 rounded-full bg-slate-950/70 hover:bg-slate-800 text-slate-300 hover:text-white flex items-center justify-center border border-slate-700/60 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header / Banner */}
        <div className="relative h-60 sm:h-72 w-full overflow-hidden shrink-0">
          <img
            src={movie.posterUrl}
            alt={movie.title}
            className="w-full h-full object-cover object-top"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/60 to-transparent" />
          <div className="absolute bottom-4 left-6 right-6">
            <div className="flex items-center gap-2 mb-2">
              <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-rose-600 text-white">
                {movie.ageRating || 'T13'}
              </span>
              <span className="text-xs text-slate-300 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-rose-400" /> {movie.durationMinutes} phút
              </span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-white">{movie.title}</h2>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Synopsis */}
          <div>
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5">Nội dung</h4>
            <p className="text-sm text-slate-300 leading-relaxed">{movie.synopsis}</p>
          </div>

          {/* Director & Cast */}
          {(movie.director || movie.actors) && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs bg-slate-950/50 p-4 rounded-xl border border-slate-800/80">
              {movie.director && (
                <div>
                  <span className="text-slate-500 font-semibold block mb-0.5">Đạo diễn:</span>
                  <span className="text-slate-200 font-medium">{movie.director}</span>
                </div>
              )}
              {movie.actors && (
                <div>
                  <span className="text-slate-500 font-semibold block mb-0.5">Diễn viên:</span>
                  <span className="text-slate-200 font-medium">{movie.actors}</span>
                </div>
              )}
            </div>
          )}

          {/* Showtimes Selection */}
          <div className="pt-2 border-t border-slate-800">
            <h3 className="text-base font-extrabold text-white flex items-center gap-2 mb-4">
              <Calendar className="w-4 h-4 text-rose-400" />
              Chọn Lịch Chiếu & Suất Chiếu
            </h3>

            {loading ? (
              <div className="text-center py-8 text-sm text-slate-400">Đang tải lịch chiếu...</div>
            ) : showtimes.length === 0 ? (
              <div className="text-center py-8 bg-slate-950/40 rounded-xl border border-slate-800/60">
                <AlertCircle className="w-8 h-8 text-amber-500 mx-auto mb-2" />
                <p className="text-sm font-medium text-slate-300">Chưa có suất chiếu nào mở bán cho phim này.</p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Cinema Tabs */}
                <div className="flex gap-2 overflow-x-auto pb-1">
                  {cinemas.map((c) => (
                    <button
                      key={c.id}
                      onClick={() => setSelectedCinema(c.id)}
                      className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                        selectedCinema === c.id
                          ? 'bg-rose-600 text-white'
                          : 'bg-slate-800/70 hover:bg-slate-800 text-slate-300'
                      }`}
                    >
                      <MapPin className="w-3 h-3" />
                      {c.name}
                    </button>
                  ))}
                </div>

                {/* Showtime Slots */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {showtimes.map((st) => (
                    <button
                      key={st.id}
                      onClick={() => onSelectShowtime(st, movie)}
                      className="p-3 rounded-xl bg-slate-950 hover:bg-rose-950/30 border border-slate-800 hover:border-rose-500/50 text-left transition-all group flex flex-col justify-between"
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-base font-black text-rose-400 group-hover:text-rose-300">
                          {st.startTime || '19:30'}
                        </span>
                        <span className="text-[10px] font-semibold uppercase px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">
                          {st.screenName || 'Rạp 1'}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-400 flex items-center justify-between mt-2 pt-2 border-t border-slate-800/60">
                        <span>Giá vé từ:</span>
                        <span className="font-bold text-slate-200">
                          {(st.basePrice || 95000).toLocaleString('vi-VN')} đ
                        </span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
