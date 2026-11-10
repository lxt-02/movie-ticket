import React, { useState, useEffect } from 'react';
import { X, ShieldCheck, Search, Lock, Unlock, UserCheck, AlertTriangle, RefreshCw } from 'lucide-react';
import { searchUsers, changeUserStatus } from '../api';

export default function AdminPanelModal({ onClose }) {
  const [users, setUsers] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [msg, setMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const fetchUsersList = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const data = await searchUsers(keyword, roleFilter, statusFilter);
      setUsers(data?.items || (Array.isArray(data) ? data : []));
    } catch (err) {
      setErrorMsg(err.message || 'Không thể tải danh sách người dùng. Hãy chắc chắn bạn đang đăng nhập tài khoản ADMIN.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsersList();
  }, [roleFilter, statusFilter]);

  const handleToggleStatus = async (user) => {
    const newStatus = user.status === 'BLOCKED' ? 'ACTIVE' : 'BLOCKED';
    setActionLoadingId(user.id);
    setMsg('');
    setErrorMsg('');
    try {
      await changeUserStatus(user.id, newStatus);
      setMsg(`Đã chuyển tài khoản ${user.email} sang trạng thái ${newStatus}!`);
      // Update local state
      setUsers(users.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u)));
    } catch (err) {
      setErrorMsg(err.message || 'Lỗi thay đổi trạng thái.');
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-4xl bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-rose-600/20 border border-rose-500/40 flex items-center justify-center text-rose-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-white">Quản Trị Người Dùng (Admin Dashboard)</h3>
              <p className="text-[11px] text-slate-400">Quản lý phân quyền và khóa tài khoản vi phạm</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Toolbar: Search & Filters */}
        <div className="p-4 bg-slate-950/40 border-b border-slate-800/80 flex flex-wrap items-center gap-3">
          {/* Search Bar */}
          <div className="flex-1 min-w-[200px] relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && fetchUsersList()}
              placeholder="Tìm theo tên hoặc email..."
              className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-rose-500"
            />
          </div>

          {/* Role Filter */}
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-slate-300 rounded-xl px-3 py-1.5 text-xs focus:outline-none focus:border-rose-500"
          >
            <option value="">Tất cả Vai trò</option>
            <option value="CUSTOMER">Khách hàng (CUSTOMER)</option>
            <option value="STAFF">Nhân viên (STAFF)</option>
            <option value="ADMIN">Quản trị (ADMIN)</option>
          </select>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 text-slate-300 rounded-xl px-3 py-1.5 text-xs focus:outline-none focus:border-rose-500"
          >
            <option value="">Tất cả Trạng thái</option>
            <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
            <option value="BLOCKED">Đã khóa (BLOCKED)</option>
          </select>

          <button
            onClick={fetchUsersList}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors"
            title="Làm mới"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Alerts */}
        {msg && (
          <div className="mx-6 mt-3 p-2.5 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs">
            {msg}
          </div>
        )}
        {errorMsg && (
          <div className="mx-6 mt-3 p-2.5 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-300 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Users Table */}
        <div className="p-6 overflow-y-auto flex-1">
          {loading ? (
            <div className="text-center py-16 text-sm text-slate-400">Đang nạp danh sách tài khoản...</div>
          ) : users.length === 0 ? (
            <div className="text-center py-16 bg-slate-950/40 rounded-2xl border border-slate-800">
              <UserCheck className="w-10 h-10 text-slate-600 mx-auto mb-2" />
              <p className="text-sm font-semibold text-slate-300">Không tìm thấy tài khoản nào phù hợp.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
                    <th className="py-3 px-3">Người dùng</th>
                    <th className="py-3 px-3">Email</th>
                    <th className="py-3 px-3">Vai trò</th>
                    <th className="py-3 px-3">Trạng thái</th>
                    <th className="py-3 px-3 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {users.map((u) => {
                    const isBlocked = u.status === 'BLOCKED';
                    const isAdmin = u.role === 'ADMIN';

                    return (
                      <tr key={u.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-2.5">
                            <img
                              src={u.avatarUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100'}
                              alt=""
                              className="w-8 h-8 rounded-full object-cover border border-slate-700"
                            />
                            <div>
                              <span className="font-bold text-white block">{u.fullName || 'Khách hàng'}</span>
                              <span className="text-[10px] font-mono text-slate-500">{u.id?.substring(0, 13)}...</span>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-3 text-slate-300">{u.email}</td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-extrabold uppercase ${
                              u.role === 'ADMIN'
                                ? 'bg-rose-950 text-rose-300 border border-rose-800'
                                : u.role === 'STAFF'
                                ? 'bg-blue-950 text-blue-300 border border-blue-800'
                                : 'bg-slate-800 text-slate-300'
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                              isBlocked
                                ? 'bg-rose-900/40 text-rose-400 border border-rose-700'
                                : 'bg-emerald-950 text-emerald-300 border border-emerald-700'
                            }`}
                          >
                            {u.status}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-right">
                          {!isAdmin && (
                            <button
                              disabled={actionLoadingId === u.id}
                              onClick={() => handleToggleStatus(u)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-semibold inline-flex items-center gap-1.5 transition-colors ${
                                isBlocked
                                  ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                                  : 'bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-800'
                              }`}
                            >
                              {isBlocked ? (
                                <>
                                  <Unlock className="w-3.5 h-3.5" />
                                  Mở khóa
                                </>
                              ) : (
                                <>
                                  <Lock className="w-3.5 h-3.5" />
                                  Khóa tài khoản
                                </>
                              )}
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
