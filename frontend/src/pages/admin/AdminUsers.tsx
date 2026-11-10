import { useState } from 'react';
import { usersApi } from '../../api/endpoints';
import type { Role, UserStatus } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { ErrorBox, Loading, Pagination, StatusBadge, useAction, useAsync } from '../../components/ui';
import { fmtDateTime, label } from '../../utils/format';

/** ADMIN: GET /users?q&role&status, PATCH /users/{id}/status (ACTIVE | BLOCKED). Roles are managed in Keycloak. */
export default function AdminUsers() {
  const { profile } = useAuth();
  const [q, setQ] = useState('');
  const [role, setRole] = useState<Role | ''>('');
  const [status, setStatus] = useState<UserStatus | ''>('');
  const [page, setPage] = useState(0);
  const list = useAsync(() => usersApi.list({ q, role: role || undefined, status: status || undefined, page, size: 20 }), [q, role, status, page]);
  const action = useAction();

  return (
    <>
      <h1>Người dùng</h1>
      <div className="row">
        <input placeholder="Email hoặc tên…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <select value={role} onChange={(e) => { setRole(e.target.value as Role | ''); setPage(0); }}>
          <option value="">Mọi vai trò</option>
          {(['CUSTOMER', 'STAFF', 'ADMIN'] as const).map((r) => <option key={r} value={r}>{label(r)}</option>)}
        </select>
        <select value={status} onChange={(e) => { setStatus(e.target.value as UserStatus | ''); setPage(0); }}>
          <option value="">Mọi trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="BLOCKED">Bị khoá</option>
        </select>
      </div>
      <p className="muted small">Vai trò được gán trong Keycloak; người bị khoá vẫn đăng nhập được nhưng không đặt vé.</p>
      <ErrorBox error={list.error ?? action.error} />
      {list.loading ? <Loading /> : (
        <table className="table">
          <thead><tr><th>Email</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th>Tạo lúc</th><th /></tr></thead>
          <tbody>
            {list.data?.items.map((u) => (
              <tr key={u.id}>
                <td>{u.email}</td>
                <td>{u.fullName}</td>
                <td>{label(u.role)}</td>
                <td><StatusBadge value={u.status} /></td>
                <td>{fmtDateTime(u.createdAt)}</td>
                <td>
                  {u.id !== profile?.id && (
                    <button className={`small ${u.status === 'ACTIVE' ? 'danger' : ''}`} disabled={action.busy}
                      onClick={() => action.run(async () => {
                        const next: UserStatus = u.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
                        if (next === 'BLOCKED' && !confirm(`Khoá tài khoản ${u.email}?`)) return;
                        const updated = await usersApi.setStatus(u.id, next);
                        list.setData((d) => d && { ...d, items: d.items.map((x) => (x.id === u.id ? updated : x)) });
                      })}>
                      {u.status === 'ACTIVE' ? 'Khoá' : 'Mở khoá'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />}
    </>
  );
}
