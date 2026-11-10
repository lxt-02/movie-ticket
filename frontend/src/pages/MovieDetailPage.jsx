import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Clock,
  Calendar,
  MapPin,
  Ticket,
  Star,
  Film,
  ArrowLeft,
  Sparkles,
  Tv,
  CheckCircle2
} from 'lucide-react';
import { getMovieDetails, getShowtimes, getCinemas, isAuthenticated } from '../api';

export default function MovieDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [movie, setMovie] = useState(null);
  const [showtimes, setShowtimes] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setLoading(true);
    Promise.all([getMovieDetails(id), getShowtimes(id), getCinemas()])
      .then(([m, sts, cns]) => {
        setMovie(m);
        setShowtimes(sts || []);
        setCinemas(cns || []);
        if (cns && cns.length > 0) setSelectedCinemaId(cns[0].id);
      })
      .finally(() => setLoading(false));
  }, [id]);

  const handleSelectShowtime = (st) => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    navigate(`/booking/${st.id}`);
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="max-w-4xl mx-auto py-20 text-center">
        <h2 className="text-xl font-bold text-slate-800">Không tìm thấy thông tin phim</h2>
        <Link to="/" className="text-red-600 font-bold mt-4 inline-block hover:underline">
          &larr; Về trang chủ
        </Link>
      </div>
    );
  }

  const filteredShowtimes = showtimes.filter(
    (st) => !selectedCinemaId || st.cinemaId === selectedCinemaId || !st.cinemaId
  );

  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 animate-in fade-in duration-300 space-y-10">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white hover:bg-slate-100 border border-slate-200 text-xs font-bold text-slate-700 transition-all shadow-sm"
      >
        <ArrowLeft className="w-4 h-4 text-red-600" />
        <span>Quay Lại</span>
      </button>

      {/* Hero Movie Info Card */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-lg grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Left: Poster */}
        <div className="aspect-[3/4] rounded-2xl overflow-hidden shadow-xl border border-slate-200 relative bg-slate-100">
          <img
            src={movie.posterUrl}
            alt={movie.title}
            className="w-full h-full object-cover"
          />
          <div className="absolute top-3 left-3 px-2.5 py-1 rounded-lg bg-red-600 text-white font-black text-xs shadow-md">
            {movie.ageRating || 'T13'}
          </div>
        </div>

        {/* Right: Details */}
        <div className="md:col-span-2 flex flex-col justify-between space-y-6">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-50 text-red-700 border border-red-200">
                ĐANG CHIẾU TẠI RẠP
              </span>
              <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200 flex items-center gap-1">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" /> 4.9/5
              </span>
              {movie.format && (
                <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
                  {movie.format}
                </span>
              )}
            </div>

            <h1 className="text-3xl sm:text-4xl font-black text-slate-900 tracking-tight uppercase mb-1">
              {movie.title}
            </h1>
            {movie.originalTitle && (
              <p className="text-slate-500 text-sm italic mb-4">{movie.originalTitle}</p>
            )}

            {/* Meta Row */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 py-4 border-y border-slate-100 text-xs">
              <div>
                <span className="text-slate-500 block mb-0.5">Thời lượng:</span>
                <span className="font-bold text-slate-800 flex items-center gap-1 text-sm">
                  <Clock className="w-4 h-4 text-red-600" /> {movie.durationMinutes} phút
                </span>
              </div>
              <div>
                <span className="text-slate-500 block mb-0.5">Khởi chiếu:</span>
                <span className="font-bold text-slate-800 flex items-center gap-1 text-sm">
                  <Calendar className="w-4 h-4 text-red-600" /> {movie.releaseDate || 'Đang chiếu'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block mb-0.5">Thể loại:</span>
                <span className="font-bold text-slate-800 text-sm">
                  {Array.isArray(movie.genres) ? movie.genres.join(', ') : 'Hành Động, Hài'}
                </span>
              </div>
            </div>

            {/* Synopsis */}
            <div className="mt-4">
              <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-2">
                Nội dung phim
              </h3>
              <p className="text-slate-600 text-sm leading-relaxed">
                {movie.synopsis || 'Tác phẩm điện ảnh hấp dẫn với trải nghiệm hình ảnh và âm thanh sống động.'}
              </p>
            </div>

            {/* Cast & Director */}
            <div className="mt-4 pt-4 border-t border-slate-100 text-xs space-y-1">
              <p><strong className="text-slate-900">Đạo diễn:</strong> {movie.director || 'Đang cập nhật'}</p>
              <p><strong className="text-slate-900">Diễn viên:</strong> {movie.actors || 'Đang cập nhật'}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Showtime Picker Section */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-lg space-y-6">
        <div>
          <h2 className="text-xl font-black text-slate-900 uppercase tracking-tight flex items-center gap-2">
            <Ticket className="w-5 h-5 text-red-600" />
            Lịch Chiếu & Suất Chiếu Hôm Nay
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            Chọn cụm rạp và giờ chiếu phù hợp để tiến hành chọn vị trí ghế ngồi
          </p>
        </div>

        {/* Cinema Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {cinemas.map((c) => (
            <button
              key={c.id}
              onClick={() => setSelectedCinemaId(c.id)}
              className={`px-4 py-2.5 rounded-2xl text-xs font-bold whitespace-nowrap transition-all border flex items-center gap-2 ${
                selectedCinemaId === c.id
                  ? 'bg-red-600 border-red-600 text-white shadow-md shadow-red-600/30'
                  : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
              }`}
            >
              <MapPin className="w-3.5 h-3.5" />
              <span>{c.name}</span>
            </button>
          ))}
        </div>

        {/* Showtime Grid */}
        <div>
          <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-3">
            Các suất chiếu có sẵn:
          </h3>
          {filteredShowtimes.length === 0 ? (
            <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-200 text-xs text-slate-500">
              Hiện chưa có suất chiếu nào được mở bán tại rạp này.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
              {filteredShowtimes.map((st) => (
                <button
                  key={st.id}
                  onClick={() => handleSelectShowtime(st)}
                  className="group p-3.5 rounded-2xl bg-slate-50 hover:bg-red-50 border border-slate-200 hover:border-red-400 text-center transition-all hover:scale-105 active:scale-95 shadow-sm"
                >
                  <span className="text-lg font-black text-slate-900 group-hover:text-red-600 block font-mono">
                    {st.startTime || '19:30'}
                  </span>
                  <span className="text-[10px] font-semibold text-slate-500 group-hover:text-red-500 block mt-0.5">
                    {st.screenName || 'Phòng 1'}
                  </span>
                  <span className="text-[10px] font-bold text-emerald-600 block mt-1">
                    {Number(st.basePrice || 95000).toLocaleString()} đ
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}