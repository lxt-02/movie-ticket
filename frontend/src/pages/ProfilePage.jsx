import React, { useState, useRef } from 'react';
import {
  User,
  Mail,
  Phone,
  Calendar,
  Camera,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Save,
  Sparkles,
  Award,
  CreditCard,
  MapPin,
  Heart,
  QrCode,
  Upload
} from 'lucide-react';
import { updateUserProfile } from '../api';

export default function ProfilePage({ currentUserProfile, onProfileUpdated }) {
  const fileInputRef = useRef(null);

  // Profile Form States
  const [fullName, setFullName] = useState(
    currentUserProfile?.fullName || localStorage.getItem('userFullName') || 'Lê Gia Thành'
  );
  const [phone, setPhone] = useState(
    currentUserProfile?.phone || localStorage.getItem('userPhone') || '0912 345 678'
  );
  const [dob, setDob] = useState(
    currentUserProfile?.dob || localStorage.getItem('userDob') || '2002-05-15'
  );
  const [gender, setGender] = useState(
    currentUserProfile?.gender || localStorage.getItem('userGender') || 'NAM'
  );
  const [favoriteCinema, setFavoriteCinema] = useState(
    currentUserProfile?.favoriteCinema || localStorage.getItem('userFavoriteCinema') || 'MTK Landmark 81'
  );
  const [avatarUrl, setAvatarUrl] = useState(
    currentUserProfile?.avatarUrl ||
      localStorage.getItem('userAvatar') ||
      'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200'
  );

  const [favoriteGenres, setFavoriteGenres] = useState(() => {
    const saved = localStorage.getItem('userGenres');
    return saved ? JSON.parse(saved) : ['Hành Động', 'Khoa Học Viễn Tưởng', 'Hài'];
  });

  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Handle direct file upload when clicking avatar
  const handleAvatarClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        setErrorMsg('Vui lòng chọn ảnh dung lượng dưới 5MB!');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        const base64 = event.target?.result;
        if (base64) {
          setAvatarUrl(base64);
          localStorage.setItem('userAvatar', base64);
          setMsg('Đã cập nhật ảnh đại diện mới thành công! Nhớ bấm "Lưu Hồ Sơ" để lưu lại.');
          setTimeout(() => setMsg(''), 4000);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const toggleGenre = (genre) => {
    if (favoriteGenres.includes(genre)) {
      setFavoriteGenres(favoriteGenres.filter((g) => g !== genre));
    } else {
      setFavoriteGenres([...favoriteGenres, genre]);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMsg('');
    setErrorMsg('');

    // Persist additional metadata to localStorage
    localStorage.setItem('userFullName', fullName);
    localStorage.setItem('userPhone', phone);
    localStorage.setItem('userDob', dob);
    localStorage.setItem('userGender', gender);
    localStorage.setItem('userFavoriteCinema', favoriteCinema);
    localStorage.setItem('userAvatar', avatarUrl);
    localStorage.setItem('userGenres', JSON.stringify(favoriteGenres));

    try {
      const updated = await updateUserProfile(fullName, avatarUrl);
      setMsg('Đã lưu toàn bộ thông tin hồ sơ thành công!');
      if (onProfileUpdated) onProfileUpdated({ ...updated, fullName, avatarUrl });
    } catch (err) {
      // Local fallback
      setMsg('Đã lưu thông tin hồ sơ cá nhân thành công!');
      if (onProfileUpdated) {
        onProfileUpdated({
          ...(currentUserProfile || {}),
          fullName,
          avatarUrl,
        });
      }
    } finally {
      setLoading(false);
      setTimeout(() => setMsg(''), 4500);
    }
  };

  const allGenresList = [
    'Hành Động',
    'Khoa Học Viễn Tưởng',
    'Hài Hước',
    'Phiêu Lưu',
    'Gia Đình',
    'Tâm Lý',
    'Kinh Dị',
    'Hoạt Hình',
    'Tình Cảm'
  ];

  const cinemaList = [
    'MTK Landmark 81 (TP.HCM)',
    'MTK Vincom Đồng Khởi (TP.HCM)',
    'MTK Royal City (Hà Nội)',
    'MTK Bà Triệu (Hà Nội)',
    'MTK Vincom Đà Nẵng',
    'MTK Ninh Kiều Cần Thơ'
  ];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 pt-8 space-y-8 animate-in fade-in duration-300">
        {/* Header Title */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-red-100 text-red-700 inline-block mb-1">
              MTK MEMBERSHIP ACCOUNT
            </span>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 uppercase tracking-tight">
              HỒ SƠ THÀNH VIÊN MTK
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Quản lý thông tin định danh, tùy chỉnh ảnh đại diện và thẻ thành viên rạp
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold flex items-center gap-1.5 shadow-xs">
              <Award className="w-4 h-4 text-amber-600" />
              <span>Hạng Thẻ: <strong>MTK VIP Gold</strong></span>
            </span>
          </div>
        </div>

        {/* Notifications */}
        {msg && (
          <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs sm:text-sm flex items-center gap-2.5 shadow-sm animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="font-medium">{msg}</span>
          </div>
        )}
        {errorMsg && (
          <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs sm:text-sm flex items-center gap-2.5 shadow-sm animate-in fade-in">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
            <span className="font-medium">{errorMsg}</span>
          </div>
        )}

        {/* 1. DIGITAL MTK VIP MEMBERSHIP CARD */}
        <div className="relative rounded-3xl p-6 sm:p-8 bg-gradient-to-r from-slate-900 via-zinc-900 to-red-950 text-white shadow-2xl overflow-hidden border border-slate-800">
          <div className="relative z-10 flex flex-col justify-between h-48 sm:h-52">
            {/* Top row */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-xl bg-red-600 flex items-center justify-center font-black text-white text-base shadow-md">
                  MTK
                </div>
                <div>
                  <h3 className="text-sm font-black tracking-widest uppercase">MTK CINEMAS VIP CLUB</h3>
                  <p className="text-[10px] text-amber-400 font-bold uppercase tracking-wider">HẠNG THẺ GOLD MEMBER</p>
                </div>
              </div>
              <Sparkles className="w-6 h-6 text-amber-400" />
            </div>

            {/* Middle row */}
            <div className="flex items-center justify-between text-xs sm:text-sm">
              <div>
                <span className="text-[10px] text-slate-400 uppercase tracking-widest block">Chủ thẻ thành viên</span>
                <span className="font-black text-base sm:text-lg uppercase text-white tracking-wide">
                  {fullName || 'LÊ GIA THÀNH'}
                </span>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-slate-400 uppercase tracking-widest block">Điểm tích lũy</span>
                <span className="font-mono font-black text-base sm:text-lg text-amber-400">
                  350 Stars ★
                </span>
              </div>
            </div>

            {/* Bottom row: Card Number & Barcode simulation */}
            <div className="flex items-center justify-between pt-3 border-t border-white/10 text-xs text-slate-400 font-mono">
              <span>MTK-8829-2026-VIP</span>
              <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800">
                ACTIVE • TỰ ĐỘNG GIẢM 10% BẮP NƯỚC
              </span>
            </div>
          </div>

          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-10 translate-y-10">
            <Award className="w-72 h-72" />
          </div>
        </div>

        {/* 2. MAIN PROFILE FORM & DIRECT AVATAR CLICK */}
        <form onSubmit={handleSave} className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-sm space-y-8">
          {/* Avatar Section: Click image to edit directly */}
          <div>
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-wider mb-4 flex items-center gap-2">
              <Camera className="w-4 h-4 text-red-600" />
              Ảnh Đại Diện (Click trực tiếp vào ảnh để đổi)
            </h3>

            <div className="flex flex-col sm:flex-row items-center gap-6 p-4 rounded-2xl bg-slate-50 border border-slate-200">
              {/* Hidden file input */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="hidden"
              />

              {/* Avatar Click Target */}
              <div
                onClick={handleAvatarClick}
                className="relative group cursor-pointer w-28 h-28 rounded-3xl overflow-hidden border-3 border-red-500 shadow-md transition-all hover:scale-105 shrink-0"
                title="Bấm vào đây để chọn ảnh từ máy tính của bạn"
              >
                <img
                  src={avatarUrl}
                  alt="User Avatar"
                  className="w-full h-full object-cover group-hover:brightness-75 transition-all"
                />

                {/* Hover Camera Overlay */}
                <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center text-white transition-opacity">
                  <Camera className="w-6 h-6 mb-1" />
                  <span className="text-[10px] font-black uppercase text-center px-1">Đổi ảnh</span>
                </div>

                {/* Small Camera Badge */}
                <div className="absolute bottom-1 right-1 w-6 h-6 rounded-full bg-red-600 text-white flex items-center justify-center shadow-md">
                  <Camera className="w-3.5 h-3.5" />
                </div>
              </div>

              {/* Avatar Instructions & Link input */}
              <div className="space-y-2 text-center sm:text-left flex-1">
                <div className="flex flex-wrap items-center gap-2 justify-center sm:justify-start">
                  <button
                    type="button"
                    onClick={handleAvatarClick}
                    className="px-3.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-sm transition-colors cursor-pointer"
                  >
                    <Upload className="w-3.5 h-3.5" />
                    Tải ảnh từ máy tính
                  </button>
                  <span className="text-xs text-slate-400">hoặc dán đường dẫn ảnh:</span>
                </div>

                <input
                  type="url"
                  value={avatarUrl.startsWith('data:') ? '' : avatarUrl}
                  onChange={(e) => setAvatarUrl(e.target.value)}
                  placeholder="https://images.unsplash.com/... (dán URL ảnh)"
                  className="w-full bg-white border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-red-500"
                />
                <p className="text-[11px] text-slate-500">
                  Hỗ trợ định dạng JPG, PNG, WebP (Tối đa 5MB). Ảnh sẽ tự động đồng bộ lên thanh Navbar.
                </p>
              </div>
            </div>
          </div>

          {/* Form Fields: Personal Information */}
          <div className="space-y-4">
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-wider pb-2 border-b border-slate-100 flex items-center gap-2">
              <User className="w-4 h-4 text-red-600" />
              Thông Tin Định Danh
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Họ và Tên *
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Lê Gia Thành"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-medium"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Số Điện Thoại *
                </label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="tel"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="0912 345 678"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-medium"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Địa Chỉ Email
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    disabled
                    value={currentUserProfile?.email || localStorage.getItem('userEmail') || 'customer@cgv.vn'}
                    className="w-full bg-slate-100 border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-500 cursor-not-allowed"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Ngày Sinh
                </label>
                <div className="relative">
                  <Calendar className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="date"
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-medium"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Giới Tính
                </label>
                <select
                  value={gender}
                  onChange={(e) => setGender(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-medium"
                >
                  <option value="NAM">Nam</option>
                  <option value="NU">Nữ</option>
                  <option value="KHAC">Khác</option>
                </select>
              </div>
            </div>
          </div>

          {/* Preferences: Favorite Cinema & Genres */}
          <div className="space-y-4">
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-wider pb-2 border-b border-slate-100 flex items-center gap-2">
              <Heart className="w-4 h-4 text-red-600" />
              Sở Thích Xem Phim & Cụm Rạp MTK
            </h3>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1.5">
                Cụm Rạp Quen Thuộc (Ưu tiên hiển thị lịch chiếu)
              </label>
              <div className="relative">
                <MapPin className="w-4 h-4 text-red-600 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <select
                  value={favoriteCinema}
                  onChange={(e) => setFavoriteCinema(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 focus:outline-none focus:border-red-500 focus:bg-white transition-all font-medium"
                >
                  {cinemaList.map((c, i) => (
                    <option key={i} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-2">
                Thể Loại Phim Yêu Thích (Chọn các thể loại bạn hay xem):
              </label>
              <div className="flex flex-wrap gap-2">
                {allGenresList.map((g) => {
                  const isSelected = favoriteGenres.includes(g);
                  return (
                    <button
                      key={g}
                      type="button"
                      onClick={() => toggleGenre(g)}
                      className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-red-600 text-white shadow-sm shadow-red-200 scale-105'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200 border border-slate-200/80'
                      }`}
                    >
                      {isSelected ? '✓ ' : '+ '}
                      {g}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* System Role Badge */}
          <div className="pt-2 flex items-center justify-between text-xs text-slate-500 border-t border-slate-100">
            <span>Vai trò tài khoản:</span>
            <span className="px-3 py-1 rounded-full bg-red-100 text-red-700 font-bold text-xs uppercase tracking-wider">
              {currentUserProfile?.role || localStorage.getItem('userRole') || 'CUSTOMER'}
            </span>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-2xl bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-md shadow-red-200 transition-all hover:scale-[1.01] active:scale-95 cursor-pointer"
          >
            <Save className="w-4 h-4" />
            <span>{loading ? 'Đang lưu...' : 'Lưu Thay Đổi Hồ Sơ'}</span>
          </button>
        </form>
      </div>
    </div>
  );
}
