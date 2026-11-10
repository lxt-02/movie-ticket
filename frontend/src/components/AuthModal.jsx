import React, { useState } from 'react';
import {
  X,
  Lock,
  User,
  Mail,
  Phone,
  ShieldAlert,
  Sparkles,
  Key,
  ArrowRight,
  CheckCircle2,
  Ticket,
  ChevronRight
} from 'lucide-react';
import { loginKeycloak, registerAccount } from '../api';

export default function AuthModal({ onClose, onLoginSuccess, initialTab = 'login', isFullScreen = false }) {
  const [activeTab, setActiveTab] = useState(initialTab); // 'login' | 'register'
  
  // Login form state
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  
  // Register form state
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regPhone, setRegPhone] = useState('');

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setLoading(true);

    try {
      await loginKeycloak(username, password);
      setSuccessMsg('Đăng nhập thành công!');
      setTimeout(() => {
        onLoginSuccess();
        if (onClose) onClose();
      }, 500);
    } catch (err) {
      setErrorMsg(err.message || 'Tài khoản hoặc mật khẩu không chính xác!');
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setLoading(true);

    try {
      await registerAccount({
        fullName: regFullName,
        email: regEmail,
        username: regUsername,
        password: regPassword,
        phone: regPhone,
      });
      setSuccessMsg('Đăng ký tài khoản thành viên CGV thành công!');
      setTimeout(() => {
        onLoginSuccess();
        if (onClose) onClose();
      }, 600);
    } catch (err) {
      setErrorMsg(err.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickAccount = (u, p) => {
    setUsername(u);
    setPassword(p);
    setErrorMsg('');
  };

  const content = (
    <div className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden p-6 sm:p-8">
      {/* Top CGV Ribbon Bar */}
      <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-red-600 via-rose-500 to-amber-500" />

      {/* Close Button if modal */}
      {onClose && (
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition-colors z-20"
        >
          <X className="w-4 h-4" />
        </button>
      )}

      {/* Brand Header */}
      <div className="text-center mb-6 pt-2">
        <div className="inline-flex items-center justify-center px-4 py-2 rounded-2xl bg-gradient-to-br from-red-600 to-rose-700 shadow-xl shadow-red-950 border border-red-500/40 mx-auto mb-3">
          <span className="font-black text-lg text-white tracking-widest">MTK</span>
        </div>
        <h3 className="text-2xl font-black text-white uppercase tracking-tight">
          {activeTab === 'register' ? 'Đăng Ký Thành Viên' : 'Đăng Nhập Tài Khoản'}
        </h3>
        <p className="text-xs text-neutral-400 mt-1">
          Hệ thống rạp chiếu phim MTK Cinemas Vietnam
        </p>

        {/* Tab Switcher */}
        <div className="mt-5 grid grid-cols-2 p-1 bg-neutral-950 rounded-2xl border border-neutral-800 text-xs font-bold">
          <button
            type="button"
            onClick={() => {
              setActiveTab('login');
              setErrorMsg('');
              setSuccessMsg('');
            }}
            className={`py-2 rounded-xl transition-all ${
              activeTab === 'login'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/40'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Đăng Nhập
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab('register');
              setErrorMsg('');
              setSuccessMsg('');
            }}
            className={`py-2 rounded-xl transition-all ${
              activeTab === 'register'
                ? 'bg-red-600 text-white shadow-md shadow-red-900/40'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            Đăng Ký Thành Viên
          </button>
        </div>
      </div>

      {/* Error & Success Notification */}
      {errorMsg && (
        <div className="mb-4 p-3 rounded-xl bg-red-950/60 border border-red-800 text-red-300 text-xs flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 shrink-0 text-red-400" />
          <span>{errorMsg}</span>
        </div>
      )}
      {successMsg && (
        <div className="mb-4 p-3 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* LOGIN TAB CONTENT */}
      {activeTab === 'login' && (
        <div>
          {/* Quick Demo Switcher Buttons */}
          <div className="mb-5 bg-neutral-950/80 p-3 rounded-2xl border border-neutral-800">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[11px] font-bold text-neutral-400">Chọn nhanh tài khoản test:</span>
              <span className="text-[10px] text-red-400">Pass: 123456</span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => fillQuickAccount('customer', '123456')}
                className={`py-1.5 px-2.5 rounded-lg border text-left transition-all ${
                  username === 'customer'
                    ? 'bg-red-950/60 border-red-600 text-red-300 font-bold'
                    : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
                }`}
              >
                👤 Khách (customer)
              </button>
              <button
                type="button"
                onClick={() => fillQuickAccount('staff', '123456')}
                className={`py-1.5 px-2.5 rounded-lg border text-left transition-all ${
                  username === 'staff'
                    ? 'bg-blue-950/60 border-blue-600 text-blue-300 font-bold'
                    : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
                }`}
              >
                🎫 Staff (nhân viên)
              </button>
              <button
                type="button"
                onClick={() => fillQuickAccount('admin', '123456')}
                className={`py-1.5 px-2.5 rounded-lg border text-left transition-all ${
                  username === 'admin'
                    ? 'bg-amber-950/60 border-amber-600 text-amber-300 font-bold'
                    : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
                }`}
              >
                🛡️ Admin (quản trị)
              </button>
              <button
                type="button"
                onClick={() => fillQuickAccount('blocked', '123456')}
                className={`py-1.5 px-2.5 rounded-lg border text-left transition-all ${
                  username === 'blocked'
                    ? 'bg-neutral-800 border-red-700 text-red-400 font-bold'
                    : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
                }`}
              >
                🚫 Blocked (bị khóa)
              </button>
            </div>
          </div>

          <form onSubmit={handleLoginSubmit} className="space-y-3.5">
            <div>
              <label className="text-xs font-semibold text-neutral-300 block mb-1">
                Tài khoản hoặc Email
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-neutral-500 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="customer, admin, staff..."
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-3 py-2.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-semibold text-neutral-300 block mb-1">
                Mật khẩu
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-neutral-500 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu..."
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-3 py-2.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-red-900/50 hover:shadow-red-900/70 transition-all"
            >
              {loading ? (
                <span>Đang xác thực...</span>
              ) : (
                <>
                  <span>Đăng Nhập</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Google OAuth Option */}
          <div className="mt-4 pt-4 border-t border-neutral-800">
            <button
              type="button"
              onClick={() => {
                fillQuickAccount('customer', '123456');
                setSuccessMsg('Đang đăng nhập bằng tài khoản Google...');
                setTimeout(async () => {
                  await loginKeycloak('customer', '123456');
                  onLoginSuccess();
                  if (onClose) onClose();
                }, 800);
              }}
              className="w-full py-2.5 px-4 rounded-xl bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-neutral-700 text-xs font-semibold text-neutral-200 flex items-center justify-center gap-2 transition-colors"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.66v3.04h3.88c2.28-2.09 3.66-5.18 3.66-9.14z"
                />
                <path
                  fill="#34A853"
                  d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.04c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.13C3.26 21.4 7.33 24 12 24z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.28 14.28c-.25-.72-.38-1.49-.38-2.28s.13-1.56.38-2.28V6.59H1.25C.45 8.19 0 9.99 0 12s.45 3.81 1.25 5.41l4.03-3.13z"
                />
                <path
                  fill="#EA4335"
                  d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.6 1.25 6.59l4.03 3.13c.95-2.83 3.6-4.97 6.72-4.97z"
                />
              </svg>
              <span>Tiếp tục với Google</span>
            </button>
          </div>
        </div>
      )}

      {/* REGISTER TAB CONTENT */}
      {activeTab === 'register' && (
        <form onSubmit={handleRegisterSubmit} className="space-y-3">
          <div>
            <label className="text-xs font-semibold text-neutral-300 block mb-1">
              Họ và tên <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-neutral-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={regFullName}
                onChange={(e) => setRegFullName(e.target.value)}
                placeholder="Ví dụ: Nguyễn Văn A"
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-neutral-300 block mb-1">
              Địa chỉ Email <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-neutral-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={regEmail}
                onChange={(e) => setRegEmail(e.target.value)}
                placeholder="email@example.com"
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-semibold text-neutral-300 block mb-1">
                Tên đăng nhập <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={regUsername}
                onChange={(e) => setRegUsername(e.target.value)}
                placeholder="username..."
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
              />
            </div>
            <div>
              <label className="text-xs font-semibold text-neutral-300 block mb-1">
                Số điện thoại
              </label>
              <input
                type="tel"
                value={regPhone}
                onChange={(e) => setRegPhone(e.target.value)}
                placeholder="0912 345 678"
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-neutral-300 block mb-1">
              Mật khẩu khởi tạo <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-neutral-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={regPassword}
                onChange={(e) => setRegPassword(e.target.value)}
                placeholder="Tối thiểu 6 ký tự..."
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-3 py-2 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500 transition-all"
              />
            </div>
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-red-900/50 hover:shadow-red-900/70 transition-all"
            >
              {loading ? (
                <span>Đang đăng ký...</span>
              ) : (
                <>
                  <span>Hoàn Tất Đăng Ký Thành Viên</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      )}

      {/* MTK Member Benefits Footer */}
      <div className="mt-6 pt-4 border-t border-neutral-800/80 text-[11px] text-neutral-400 flex items-center justify-between">
        <span className="flex items-center gap-1.5">
          <Ticket className="w-3.5 h-3.5 text-red-500" />
          Tích lũy điểm MTK Point mỗi vé
        </span>
        <span className="text-neutral-500">•</span>
        <span>Ưu đãi sinh nhật & Thứ 4 Vui Vẻ</span>
      </div>
    </div>
  );

  if (isFullScreen) {
    return (
      <div className="min-h-screen bg-neutral-950 flex items-center justify-center p-4">
        {content}
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      {content}
    </div>
  );
}