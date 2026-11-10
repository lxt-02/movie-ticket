import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { label } from '../utils/format';

export function Layout() {
  const { ready, authenticated, role, username, profile, profileError, login, register, logout, hasRole } = useAuth();

  return (
    <>
      <header className="topbar">
        <div className="container topbar-inner">
          <NavLink to="/" className="brand">🎬 MSS301 Cinema</NavLink>
          <nav>
            <NavLink to="/" end>Phim</NavLink>
            <NavLink to="/search">Tìm kiếm</NavLink>
            <NavLink to="/cinemas">Rạp</NavLink>
            {authenticated && <NavLink to="/my-bookings">Vé của tôi</NavLink>}
            {hasRole('ADMIN', 'STAFF') && <NavLink to="/ops">Vận hành</NavLink>}
            {hasRole('ADMIN') && <NavLink to="/admin">Quản trị</NavLink>}
          </nav>
          <div className="user-box">
            {!ready ? null : authenticated ? (
              <>
                <NavLink to="/profile" className="user-name">
                  {profile?.fullName || username} <span className={`role role-${role?.toLowerCase()}`}>{label(role)}</span>
                </NavLink>
                <button className="ghost" onClick={logout}>Đăng xuất</button>
              </>
            ) : (
              <>
                <button className="ghost" onClick={register}>Đăng ký</button>
                <button onClick={() => login()}>Đăng nhập</button>
              </>
            )}
          </div>
        </div>
      </header>
      {profile?.status === 'BLOCKED' && (
        <div className="banner warn">Tài khoản của bạn đang bị khoá: bạn vẫn xem được thông tin nhưng không đặt vé được.</div>
      )}
      {profileError && <div className="banner warn">Không tải được hồ sơ người dùng: {profileError}</div>}
      <main className="container">
        <Outlet />
      </main>
      <footer className="container muted small">MSS301 · FE gọi API qua gateway {import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}</footer>
    </>
  );
}
