import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { moviesApi } from '../api/endpoints';
import type { MovieStatus } from '../api/types';
import { ErrorBox, Loading, Pagination, useAsync } from '../components/ui';
import { MovieCard } from '../components/MovieCard';
import { SuggestBox } from '../components/SuggestBox';

const TABS: { value?: MovieStatus; text: string }[] = [
  { value: 'NOW_SHOWING', text: 'Đang chiếu' },
  { value: 'COMING_SOON', text: 'Sắp chiếu' },
  { value: undefined, text: 'Tất cả' },
];

/** GET /api/v1/movies (public) */
export default function HomePage() {
  const [status, setStatus] = useState<MovieStatus | undefined>('NOW_SHOWING');
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const { data, error, loading } = useAsync(() => moviesApi.list({ status, page, size: 12 }), [status, page]);

  return (
    <>
      <section className="hero">
        <h1>Đặt vé xem phim</h1>
        <SuggestBox onSubmit={(q) => navigate(`/search?q=${encodeURIComponent(q)}`)} />
      </section>
      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.text} className={status === t.value ? 'active' : ''} onClick={() => { setStatus(t.value); setPage(0); }}>
            {t.text}
          </button>
        ))}
      </div>
      <ErrorBox error={error} />
      {loading ? <Loading /> : (
        <>
          <div className="movie-grid">
            {data?.items.map((m) => (
              <MovieCard key={m.id} id={m.id} title={m.title} posterUrl={m.posterUrl} status={m.status}
                meta={`${m.durationMinutes} phút · ${m.ageRating ?? ''} · ${m.genres.map((g) => g.name).join(', ')}`} />
            ))}
          </div>
          {data?.items.length === 0 && <p className="muted">Chưa có phim.</p>}
          {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
        </>
      )}
    </>
  );
}
