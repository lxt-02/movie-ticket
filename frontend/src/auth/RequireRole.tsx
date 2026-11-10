import { useEffect, type ReactNode } from 'react';
import { useAuth } from './AuthContext';
import type { Role } from '../api/types';

/**
 * Route guard.
 * - no roles: any logged-in user
 * - roles: only those roles (ADMIN pages: ['ADMIN']; ops pages: ['ADMIN', 'STAFF'])
 * The backend checks permissions again; this only hides pages the user cannot use.
 */
export function RequireRole({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { ready, authenticated, login, hasRole } = useAuth();

  useEffect(() => {
    if (ready && !authenticated) login();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, authenticated]);

  if (!ready || !authenticated) return <p className="muted">Đang chuyển tới trang đăng nhập…</p>;
  if (roles && !hasRole(...roles)) {
    return (
      <div className="card">
        <h2>403 – Không đủ quyền</h2>
        <p>Trang này chỉ dành cho: {roles.join(', ')}.</p>
      </div>
    );
  }
  return <>{children}</>;
}
