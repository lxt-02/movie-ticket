import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { bookingsApi, showtimesApi } from '../api/endpoints';
import type { ShowtimeSeat } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { config } from '../config';
import { ErrorBox, Loading, useAsync } from '../components/ui';
import { fmtDateTime, fmtMoney, uuid } from '../utils/format';

const REFRESH_MS = 20_000;

/**
 * Seat map (public) + create booking (logged in).
 * GET /showtimes/{id}, GET /showtimes/{id}/seats, POST /bookings with Idempotency-Key (FRONTEND_API §3.1–3.2)
 */
export default function SeatSelectionPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const { authenticated, login, profile } = useAuth();
  const showtime = useAsync(() => showtimesApi.get(id), [id]);
  const seats = useAsync(() => showtimesApi.seats(id), [id]);
  const [selected, setSelected] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const [cooldown, setCooldown] = useState(0);

  // Idempotency-Key: new key for each seat set; same key when re-sending after 503/504/network error
  const keyRef = useRef<{ seatsKey: string; key: string } | null>(null);

  // refresh seat map periodically
  useEffect(() => {
    const t = setInterval(() => seats.reload(), REFRESH_MS);
    return () => clearInterval(t);
  }, [seats.reload]);

  // drop selected seats that are no longer available after a refresh
  useEffect(() => {
    if (!seats.data) return;
    const available = new Set(seats.data.filter((s) => s.status === 'AVAILABLE').map((s) => s.showtimeSeatId));
    setSelected((prev) => prev.filter((sid) => available.has(sid)));
  }, [seats.data]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  const rows = useMemo(() => {
    const map = new Map<string, ShowtimeSeat[]>();
    for (const s of seats.data ?? []) map.set(s.rowLabel, [...(map.get(s.rowLabel) ?? []), s]);
    return [...map.entries()];
  }, [seats.data]);
  const maxCol = Math.max(0, ...(seats.data ?? []).map((s) => s.seatNumber));
  const seatTypes = [...new Set((seats.data ?? []).map((s) => s.seatType))];

  const chosen = (seats.data ?? []).filter((s) => selected.includes(s.showtimeSeatId));
  const total = chosen.reduce((sum, s) => sum + Number(s.price), 0);

  const st = showtime.data;
  const salesClosed = st ? new Date(st.startTime).getTime() - Date.now() < config.bookingCutoffMinutes * 60_000 : false;
  const blocked = profile?.status === 'BLOCKED';

  const toggle = (s: ShowtimeSeat) => {
    if (s.status !== 'AVAILABLE') return;
    setError(undefined);
    setSelected((prev) => {
      if (prev.includes(s.showtimeSeatId)) return prev.filter((x) => x !== s.showtimeSeatId);
      if (prev.length >= config.maxSeatsPerBooking) {
        setError(new ApiError(400, `Chỉ được chọn tối đa ${config.maxSeatsPerBooking} ghế`));
        return prev;
      }
      return [...prev, s.showtimeSeatId];
    });
  };

  const book = async () => {
    if (!authenticated) return login();
    const seatsKey = [...selected].sort().join(',');
    if (!keyRef.current || keyRef.current.seatsKey !== seatsKey) keyRef.current = { seatsKey, key: uuid() };

    setBusy(true);
    setError(undefined);
    try {
      const booking = await bookingsApi.create(
        { showtimeId: id, seats: selected.map((showtimeSeatId) => ({ showtimeSeatId })) },
        keyRef.current.key,
      );
      keyRef.current = null;
      navigate(`/checkout/${booking.id}`);
    } catch (e) {
      setError(e);
      const err = e as ApiError;
      if (err.status === 429 || err.status === 503 || err.status === 504) setCooldown(err.retryAfter ?? 2);
      // 400 / 409: this attempt is dead, next click uses a new key
      if (err.status === 400 || err.status === 409 || err.status === 403) keyRef.current = null;
      if (err.status === 409) seats.reload();
    } finally {
      setBusy(false);
    }
  };

  if (showtime.loading) return <Loading />;
  if (!st) return <ErrorBox error={showtime.error} />;

  return (
    <div className="seat-page">
      <section>
        <div className="card">
          <h2>{st.movieTitle}</h2>
          <div>{st.cinemaName} · {st.screenName} · <strong>{fmtDateTime(st.startTime)}</strong></div>
        </div>
        <div className="screen-bar">MÀN HÌNH</div>
        {seats.loading && !seats.data ? <Loading /> : (
          <div className="seat-map" style={{ gridTemplateColumns: `2rem repeat(${maxCol}, 2.2rem)` }}>
            {rows.map(([row, list]) => (
              <Row key={row} row={row} list={list} maxCol={maxCol} selected={selected} onToggle={toggle} />
            ))}
          </div>
        )}
        <div className="legend">
          <span><i className="seat available" /> Trống</span>
          <span><i className="seat selected" /> Đang chọn</span>
          <span><i className="seat held" /> Đang có người giữ</span>
          <span><i className="seat booked" /> Đã bán</span>
          <span><i className="seat blocked" /> Không bán</span>
          {seatTypes.map((t) => <span key={t}><i className={`seat available type-${t.toLowerCase()}`} /> {t}</span>)}
        </div>
        <button className="ghost small" onClick={() => seats.reload()}>↻ Tải lại sơ đồ ghế</button>
      </section>

      <aside className="card summary">
        <h3>Ghế đã chọn</h3>
        {chosen.length === 0 ? <p className="muted">Chưa chọn ghế (tối đa {config.maxSeatsPerBooking}).</p> : (
          <ul className="plain">
            {chosen.map((s) => (
              <li key={s.showtimeSeatId}>{s.seatLabel} <span className="muted">({s.seatType})</span> <span className="right">{fmtMoney(s.price)}</span></li>
            ))}
          </ul>
        )}
        <div className="total">Tạm tính: <strong>{fmtMoney(total)}</strong></div>
        <p className="muted small">Tổng tiền chính xác do hệ thống tính khi tạo đơn.</p>
        <ErrorBox error={error} />
        {salesClosed ? <p className="warn-text">Suất chiếu đã ngừng bán vé.</p>
          : blocked ? <p className="warn-text">Tài khoản đang bị khoá, không thể đặt vé.</p>
          : (
            <button className="primary block" disabled={busy || cooldown > 0 || (authenticated && selected.length === 0)} onClick={book}>
              {!authenticated ? 'Đăng nhập để đặt vé' : busy ? 'Đang giữ ghế…' : cooldown > 0 ? `Thử lại sau ${cooldown}s` : 'Đặt vé'}
            </button>
          )}
      </aside>
    </div>
  );
}

function Row({ row, list, maxCol, selected, onToggle }: {
  row: string; list: ShowtimeSeat[]; maxCol: number; selected: string[]; onToggle: (s: ShowtimeSeat) => void;
}) {
  const byCol = new Map(list.map((s) => [s.seatNumber, s]));
  return (
    <>
      <div className="row-label">{row}</div>
      {Array.from({ length: maxCol }, (_, i) => {
        const s = byCol.get(i + 1);
        if (!s) return <div key={i} />;
        const isSel = selected.includes(s.showtimeSeatId);
        const cls = isSel ? 'selected' : s.status.toLowerCase();
        return (
          <button key={s.showtimeSeatId} className={`seat ${cls} type-${s.seatType.toLowerCase()}`}
            disabled={s.status !== 'AVAILABLE'} onClick={() => onToggle(s)}
            title={`${s.seatLabel} · ${s.seatType} · ${fmtMoney(s.price)}`}>
            {s.seatNumber}
          </button>
        );
      })}
    </>
  );
}
