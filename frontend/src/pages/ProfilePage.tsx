import { useEffect, useState } from 'react';
import { usersApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { keycloak } from '../auth/keycloak';
import { FieldError, ErrorBox, Loading, StatusBadge, useAction } from '../components/ui';
import { fmtDateTime, label } from '../utils/format';

/** GET / PUT /api/v1/users/me. Password & email are managed on Keycloak's account page. */
export default function ProfilePage() {
  const { profile, setProfile } = useAuth();
  const [fullName, setFullName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [saved, setSaved] = useState(false);
  const action = useAction();

  useEffect(() => {
    if (profile) { setFullName(profile.fullName ?? ''); setAvatarUrl(profile.avatarUrl ?? ''); }
  }, [profile]);

  if (!profile) return <Loading />;

  const save = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(false);
    action.run(async () => {
      setProfile(await usersApi.updateMe({ fullName, avatarUrl: avatarUrl || undefined }));
      setSaved(true);
    });
  };

  return (
    <div className="card narrow">
      <h1>Hồ sơ</h1>
      <dl className="info">
        <dt>Email</dt><dd>{profile.email} {profile.emailVerified ? '✔' : '(chưa xác thực)'}</dd>
        <dt>Vai trò</dt><dd>{label(profile.role)}</dd>
        <dt>Trạng thái</dt><dd><StatusBadge value={profile.status} /></dd>
        <dt>Ngày tạo</dt><dd>{fmtDateTime(profile.createdAt)}</dd>
      </dl>
      <form onSubmit={save} className="form">
        <label>Họ tên<input value={fullName} maxLength={150} required onChange={(e) => setFullName(e.target.value)} /></label>
        <FieldError error={action.error} field="fullName" />
        <label>Ảnh đại diện (URL)<input value={avatarUrl} maxLength={1000} onChange={(e) => setAvatarUrl(e.target.value)} /></label>
        <FieldError error={action.error} field="avatarUrl" />
        <button className="primary" disabled={action.busy}>Lưu</button>
        {saved && <span className="ok"> Đã lưu</span>}
      </form>
      <ErrorBox error={action.error} />
      <p className="small"><a href={keycloak.createAccountUrl()} target="_blank" rel="noreferrer">Đổi mật khẩu / email trên Keycloak ↗</a></p>
    </div>
  );
}
