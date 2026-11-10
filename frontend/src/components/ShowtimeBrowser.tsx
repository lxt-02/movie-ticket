import { useState } from 'react';
import { Link } from 'react-router-dom';
import { showtimesApi } from '../api/endpoints';
import type { Showtime } from '../api/types';
import { config } from '../config';
import { ErrorBox, Loading, useAsync } from './ui';
import { fmtMoney, fmtTime, vnDate } from '../utils/format';

/** Date tabs (Vietnam days) + showtimes grouped by cinema or by movie. GET /api/v1/showtimes (public) */
export function ShowtimeBrowser({ movieId, cinemaId, groupBy }: { movieId?: string; cinemaId?: string; groupBy: 'cinema' | 'movie' }) {
  const days = Array.from({ length: 7 }, (_, i) => vnDate(i));
  const [date, setDate] = useState(days[0]);
  const { data, error, loading } = useAsync(() => showtimesApi.list({ movieId, cinemaId, date, size: 100 }), [movieId, cinemaId, date]);

  const groups = new Map<string, Showtime[]>();
  for (const s of data?.items ?? []) {
    const key = groupBy === 'cinema' ? s.cinemaName : s.movieTitle;
    groups.set(key, [...(groups.get(key) ?? []), s]);
  }

  return (
    <div>
      <div className="tabs">
        {days.map((d, i) => (
          <button key={d} className={d === date ? 'active' : ''} onClick={() => setDate(d)}>
            {i === 0 ? 'Hôm nay' : new Date(d + 'T00:00:00Z').toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: 'UTC' })}
          </button>
        ))}
      </div>
      <ErrorBox error={error} />
      {loading ? <Loading /> : groups.size === 0 ? <p className="muted">Không có suất chiếu trong ngày này.</p> : (
        [...groups.entries()].map(([name, list]) => (
          <div key={name} className="card">
            <h3>{name}</h3>
            <div className="showtime-chips">
              {list.map((s) => {
                const closed = new Date(s.startTime).getTime() - Date.now() < config.bookingCutoffMinutes * 60_000;
                return closed ? (
                  <span key={s.id} className="chip disabled" title="Đã ngừng bán (dưới 15 phút trước giờ chiếu)">
                    {fmtTime(s.startTime)} · {s.screenName}
                  </span>
                ) : (
                  <Link key={s.id} to={`/showtimes/${s.id}`} className="chip">
                    <strong>{fmtTime(s.startTime)}</strong> · {s.screenName} · từ {fmtMoney(s.basePrice)}
                  </Link>
                );
              })}
            </div>
          </div>
        ))
      )}
    </div>
  );
}
