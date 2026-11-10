import React, { useState, useEffect } from 'react';
import { X, User, Mail, Shield, CheckCircle2, AlertCircle, Camera, Save } from 'lucide-react';
import { getCurrentUserProfile, updateUserProfile } from '../api';

const SAMPLE_AVATARS = [
  'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
  'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150',
  'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
  'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
  'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150',
  'https://images.unsplash.com/photo-1628157582853-a796fa650a6a?w=150',
];

export default function ProfileModal({ onClose, onProfileUpdated }) {
  const [profile, setProfile] = useState(null);
  const [fullName, setFullName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    getCurrentUserProfile()
      .then((data) => {
        setProfile(data);
        setFullName(data.fullName || '');
        setAvatarUrl(data.avatarUrl || SAMPLE_AVATARS[0]);
      })
      .catch((e) => setErrorMsg(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setErrorMsg('Họ và tên không được để trống.');
      return;
    }
    setSaving(true);
    setErrorMsg('');
    setSuccessMsg('');
    try {
      const updated = await updateUserProfile(fullName.trim(), avatarUrl);
      setProfile(updated);
      setSuccessMsg('Hồ sơ của bạn đã được cập nhật thành công!');
      if (onProfileUpdated) onProfileUpdated(updated);
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err) {
      setErrorMsg(err.message || 'Cập nhật thất bại.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden p-6 sm:p-8">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-2 mb-6">
          <User className="w-5 h-5 text-rose-400" />
          <h3 className="text-lg font-black text-white">Chỉnh Sửa Hồ Sơ Cá Nhân</h3>
        </div>

        {loading ? (
          <div className="text-center py-12 text-sm text-slate-400">Đang tải hồ sơ...</div>
        ) : (
          <form onSubmit={handleSave} className="space-y-5">
            {/* Messages */}
            {successMsg && (
              <div className="p-3 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-300 text-xs flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
                <span>{successMsg}</span>
              </div>
            )}
            {errorMsg && (
              <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Avatar Section */}
            <div className="flex flex-col items-center justify-center gap-3">
              <div className="relative group">
                <img
                  src={avatarUrl}
                  alt="Avatar"
                  className="w-20 h-20 rounded-full object-cover border-2 border-rose-500 shadow-lg shadow-rose-950/40"
                  onError={(e) => {
                    e.target.src = SAMPLE_AVATARS[0];
                  }}
                />
              </div>

              {/* Quick Sample Avatars */}
              <div>
                <span className="text-[11px] font-semibold text-slate-400 block text-center mb-1.5">
                  Chọn ảnh đại diện:
                </span>
                <div className="flex gap-2">
                  {SAMPLE_AVATARS.map((url, idx) => (
                    <img
                      key={idx}
                      src={url}
                      alt="Choice"
                      onClick={() => setAvatarUrl(url)}
                      className={`w-8 h-8 rounded-full object-cover cursor-pointer transition-transform hover:scale-110 border ${
                        avatarUrl === url ? 'border-rose-500 ring-2 ring-rose-500/50' : 'border-slate-800 opacity-60 hover:opacity-100'
                      }`}
                    />
                  ))}
                </div>
              </div>
            </div>

            {/* Read-Only Info */}
            <div className="grid grid-cols-2 gap-3 p-3 bg-slate-950 rounded-2xl border border-slate-800 text-xs">
              <div>
                <span className="text-slate-500 block mb-0.5">Email tài khoản:</span>
                <span className="font-semibold text-slate-200 truncate block">{profile?.email || 'Chưa cập nhật'}</span>
              </div>
              <div>
                <span className="text-slate-500 block mb-0.5">Vai trò hệ thống:</span>
                <span className="font-extrabold text-rose-400 uppercase">
                  {profile?.role || 'CUSTOMER'}
                </span>
              </div>
            </div>

            {/* Editable Fields */}
            <div>
              <label className="text-xs font-semibold text-slate-400 block mb-1">Họ và tên</label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nhập họ và tên..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-rose-500"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-400 block mb-1">Đường dẫn ảnh đại diện (Tùy chọn URL)</label>
              <input
                type="url"
                value={avatarUrl}
                onChange={(e) => setAvatarUrl(e.target.value)}
                placeholder="https://example.com/avatar.png"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-rose-500"
              />
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={saving}
                className="w-full py-3 rounded-xl bg-gradient-to-r from-rose-600 to-pink-600 hover:from-rose-500 hover:to-pink-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-rose-950/50 transition-all"
              >
                <Save className="w-4 h-4" />
                <span>{saving ? 'Đang lưu vào cơ sở dữ liệu...' : 'Lưu Thay Đổi Hồ Sơ'}</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
