import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Logo from '../components/Logo';
import { Lock, User, ShieldAlert, CheckCircle2, ArrowRight } from 'lucide-react';
import { loginKeycloak } from '../api';

export default function LoginPage({ onLoginSuccess }) {
  const [username, setUsername] = useState('customer');
  const [password, setPassword] = useState('123456');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigate = useNavigate();

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setLoading(true);

    try {
      const data = await loginKeycloak(username, password);
      setSuccessMsg('Đăng nhập thành công!');
      if (onLoginSuccess) await onLoginSuccess();
      setTimeout(() => {
        const role = localStorage.getItem('userRole');
        if (role === 'STAFF') navigate('/staff/dashboard');
        else if (role === 'ADMIN') navigate('/admin/dashboard');
        else navigate('/');
      }, 500);
    } catch (err) {
      setErrorMsg(err.message || 'Tài khoản hoặc mật khẩu không chính xác!');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickAccount = (u, p) => {
    setUsername(u);
    setPassword(p);
    setErrorMsg('');
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-slate-50">
      <div className="max-w-md w-full bg-white border border-slate-200 rounded-3xl p-8 shadow-xl space-y-6">
        <div className="text-center">
          <Logo className="justify-center mb-4" />
          <h2 className="text-2xl font-black text-slate-900 tracking-tight uppercase">
            Đăng Nhập Tài Khoản
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            Chào mừng bạn quay lại hệ thống rạp MTK Cinemas
          </p>
        </div>

        {/* Error / Success Notifications */}
        {errorMsg && (
          <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 shrink-0 text-red-600" />
            <span>{errorMsg}</span>
          </div>
        )}
        {successMsg && (
          <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Quick Demo Selector */}
        <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-200">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-slate-600">Chọn nhanh tài khoản test:</span>
            <span className="text-[10px] text-red-600 font-mono font-bold">Pass: 123456</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <button
              type="button"
              onClick={() => fillQuickAccount('customer', '123456')}
              className={`py-2 px-2.5 rounded-xl border text-left transition-all ${
                username === 'customer'
                  ? 'bg-red-50 border-red-500 text-red-700 font-bold shadow-sm'
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-100'
              }`}
            >
              👤 Khách (customer)
            </button>
            <button
              type="button"
              onClick={() => fillQuickAccount('staff', '123456')}
              className={`py-2 px-2.5 rounded-xl border text-left transition-all ${
                username === 'staff'
                  ? 'bg-blue-50 border-blue-500 text-blue-700 font-bold shadow-sm'
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-100'
              }`}
            >
              🎫 Staff (nhân viên)
            </button>
            <button
              type="button"
              onClick={() => fillQuickAccount('admin', '123456')}
              className={`py-2 px-2.5 rounded-xl border text-left transition-all ${
                username === 'admin'
                  ? 'bg-amber-50 border-amber-500 text-amber-700 font-bold shadow-sm'
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-100'
              }`}
            >
              🛡️ Admin (quản trị)
            </button>
            <button
              type="button"
              onClick={() => fillQuickAccount('blocked', '123456')}
              className={`py-2 px-2.5 rounded-xl border text-left transition-all ${
                username === 'blocked'
                  ? 'bg-rose-50 border-rose-500 text-rose-700 font-bold shadow-sm'
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-100'
              }`}
            >
              🚫 Blocked (bị khóa)
            </button>
          </div>
        </div>

        {/* Login Form */}
        <form onSubmit={handleLoginSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1.5">
              Tài khoản hoặc Email
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="customer, admin, staff..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-red-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1.5">
              Mật khẩu
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Nhập mật khẩu..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-red-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-lg shadow-red-600/30 hover:scale-[1.01] active:scale-[0.99] transition-all disabled:opacity-50"
          >
            {loading ? <span>Đang xác thực...</span> : <><span>Đăng Nhập</span><ArrowRight className="w-4 h-4" /></>}
          </button>
        </form>

        <div className="pt-2 text-center text-xs text-slate-600">
          Chưa có tài khoản thành viên?{' '}
          <Link to="/register" className="font-bold text-red-600 hover:underline">
            Đăng ký ngay
          </Link>
        </div>
      </div>
    </div>
  );
}