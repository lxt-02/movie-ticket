import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Logo from './Logo';
import {
  Film,
  Ticket,
  User,
  Search,
  ShieldCheck,
  LogIn,
  LogOut,
  Sparkles,
  ChevronDown,
  Phone,
  Building2
} from 'lucide-react';
import { logout, getUserRole, isAuthenticated } from '../api';

export default function Navbar({ onSearch, currentUserProfile }) {
  const [keyword, setKeyword] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();

  const isUserLoggedIn = isAuthenticated() && !!currentUserProfile;
  const role = isUserLoggedIn ? currentUserProfile?.role || getUserRole() : null;
  const isAdmin = role === 'ADMIN';
  const isStaff = role === 'STAFF';

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (onSearch) onSearch(keyword);
    navigate('/movies');
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/';
  };

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/90 shadow-sm transition-all">
      {/* 1. TOP UTILITY BAR (Slate-900 contrast) */}
      <div className="bg-slate-900 px-4 lg:px-8 py-1.5 text-[11px] text-slate-300">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="hidden sm:inline-flex items-center gap-1.5 text-amber-400 font-semibold">
              <Sparkles className="w-3 h-3" />
              MTK Cinemas: Trải nghiệm điện ảnh đỉnh cao chuẩn quốc tế
            </span>
            <span className="hidden md:inline text-slate-600">|</span>
            <span className="flex items-center gap-1 text-slate-300">
              <Phone className="w-3 h-3 text-red-500" />
              Hotline: <strong className="text-white">1900 6017</strong>
            </span>
          </div>

          <div className="flex items-center gap-4">
            <Link to="/features" className="hover:text-red-400 transition-colors flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-red-400" />
              <span>Đặc Trưng MTK</span>
            </Link>

            <Link to="/my-tickets" className="hover:text-red-400 transition-colors flex items-center gap-1">
              <Ticket className="w-3 h-3 text-red-400" />
              <span>Vé Của Tôi</span>
            </Link>

            {!isUserLoggedIn ? (
              <Link
                to="/login"
                className="font-bold text-red-400 hover:text-red-300 flex items-center gap-1 pl-2 border-l border-slate-700"
              >
                <LogIn className="w-3 h-3" />
                <span>ĐĂNG NHẬP / ĐĂNG KÝ</span>
              </Link>
            ) : (
              <div className="flex items-center gap-2 pl-2 border-l border-slate-700">
                <span className="text-slate-200">
                  Xin chào, <strong className="text-white">{currentUserProfile?.fullName || 'Khách'}</strong>
                </span>
                {role && (
                  <span
                    className={`px-1.5 py-0.5 rounded text-[10px] font-black uppercase ${
                      isAdmin
                        ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                        : isStaff
                        ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40'
                        : 'bg-red-500/20 text-red-300 border border-red-500/40'
                    }`}
                  >
                    {role}
                  </span>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 2. MAIN WHITE NAVIGATION BAR */}
      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-3 flex items-center justify-between gap-4">
        {/* Brand Logo */}
        <Logo onClick={() => navigate('/')} />

        {/* Categories Navigation */}
        <nav className="hidden lg:flex items-center gap-6 text-sm font-bold tracking-tight text-slate-700">
          <Link to="/movies" className="hover:text-red-600 transition-colors uppercase tracking-wider">
            Phim Đang Chiếu
          </Link>
          <Link to="/cinemas" className="hover:text-red-600 transition-colors uppercase tracking-wider">
            Cụm Rạp MTK
          </Link>
          <Link to="/features" className="hover:text-red-600 transition-colors uppercase tracking-wider">
            Đặc Trưng & Công Nghệ
          </Link>
          <Link to="/about" className="hover:text-red-600 transition-colors uppercase tracking-wider">
            Giới Thiệu
          </Link>
        </nav>

        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="hidden sm:flex flex-1 max-w-xs mx-2 relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Tìm kiếm phim, diễn viên..."
            className="w-full bg-slate-100 hover:bg-slate-50 border border-slate-200 text-xs rounded-full pl-9 pr-3 py-2 text-slate-800 placeholder-slate-400 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500/30 transition-all"
          />
        </form>

        {/* Action Buttons */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              if (!isUserLoggedIn) {
                navigate('/login');
              } else {
                navigate('/movies');
              }
            }}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-black text-xs uppercase tracking-wider shadow-md shadow-red-600/30 hover:scale-105 active:scale-95 transition-all cursor-pointer"
          >
            <Ticket className="w-4 h-4 fill-white" />
            <span>MUA VÉ NGAY</span>
          </button>

          {/* Admin Dashboard Pill */}
          {isAdmin && (
            <Link
              to="/admin/dashboard"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-300 text-xs font-bold text-amber-800 transition-colors shadow-sm"
            >
              <ShieldCheck className="w-4 h-4 text-amber-600" />
              <span className="hidden md:inline">Admin Portal</span>
            </Link>
          )}

          {/* Staff Dashboard Pill */}
          {isStaff && (
            <Link
              to="/staff/dashboard"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-300 text-xs font-bold text-blue-800 transition-colors shadow-sm"
            >
              <Ticket className="w-4 h-4 text-blue-600" />
              <span className="hidden md:inline">Quầy Soát Vé</span>
            </Link>
          )}

          {/* User Profile Menu */}
          {isUserLoggedIn ? (
            <div className="relative">
              <button
                onClick={() => setMenuOpen(!menuOpen)}
                className="flex items-center gap-2 p-1.5 pr-2 rounded-full bg-slate-100 hover:bg-slate-200 border border-slate-200 transition-all"
              >
                <img
                  src={
                    currentUserProfile?.avatarUrl ||
                    'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100'
                  }
                  alt="Avatar"
                  className="w-7 h-7 rounded-full object-cover border border-red-500"
                />
                <ChevronDown className="w-3.5 h-3.5 text-slate-600" />
              </button>

              {menuOpen && (
                <div className="absolute right-0 mt-2 w-56 bg-white border border-slate-200 rounded-2xl shadow-xl py-2 z-50 animate-in fade-in duration-150">
                  <div className="px-4 py-2 border-b border-slate-100">
                    <p className="text-xs font-bold text-slate-800 truncate">
                      {currentUserProfile?.fullName || 'Thành Viên MTK'}
                    </p>
                    <p className="text-[10px] text-slate-500 truncate">
                      {currentUserProfile?.email}
                    </p>
                  </div>

                  <Link
                    to="/profile"
                    onClick={() => setMenuOpen(false)}
                    className="w-full px-4 py-2 text-left text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                  >
                    <User className="w-3.5 h-3.5 text-slate-500" />
                    <span>Hồ Sơ Cá Nhân</span>
                  </Link>

                  <Link
                    to="/my-tickets"
                    onClick={() => setMenuOpen(false)}
                    className="w-full px-4 py-2 text-left text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                  >
                    <Ticket className="w-3.5 h-3.5 text-red-500" />
                    <span>Vé Của Tôi</span>
                  </Link>

                  {isAdmin && (
                    <Link
                      to="/admin/dashboard"
                      onClick={() => setMenuOpen(false)}
                      className="w-full px-4 py-2 text-left text-xs text-amber-700 hover:bg-amber-50 flex items-center gap-2 font-semibold"
                    >
                      <ShieldCheck className="w-3.5 h-3.5 text-amber-600" />
                      <span>Trang Quản Trị Admin</span>
                    </Link>
                  )}

                  {isStaff && (
                    <Link
                      to="/staff/dashboard"
                      onClick={() => setMenuOpen(false)}
                      className="w-full px-4 py-2 text-left text-xs text-blue-700 hover:bg-blue-50 flex items-center gap-2 font-semibold"
                    >
                      <Ticket className="w-3.5 h-3.5 text-blue-600" />
                      <span>Quầy Vận Hành Staff</span>
                    </Link>
                  )}

                  <div className="border-t border-slate-100 my-1" />

                  <button
                    onClick={handleLogout}
                    className="w-full px-4 py-2 text-left text-xs text-red-600 hover:bg-red-50 flex items-center gap-2 font-medium"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Đăng Xuất</span>
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link
              to="/login"
              className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-200 text-xs font-bold text-slate-700 transition-colors"
            >
              Đăng Nhập
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}