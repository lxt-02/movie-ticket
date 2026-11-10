import { useParams } from 'react-router-dom';
import { moviesApi } from '../api/endpoints';
import { ErrorBox, Loading, StatusBadge, useAsync } from '../components/ui';
import { ShowtimeBrowser } from '../components/ShowtimeBrowser';
import { fmtDate } from '../utils/format';

/** GET /api/v1/movies/{id} + showtimes of the movie (public) */
export default function MovieDetailPage() {
  const { id = '' } = useParams();
  const { data: m, error, loading } = useAsync(() => moviesApi.get(id), [id]);

  if (loading) return <Loading />;
  if (error || !m) return <ErrorBox error={error} />;

  return (
    <>
      <div className="movie-detail card">
        <div className="poster big">{m.posterUrl ? <img src={m.posterUrl} alt={m.title} /> : <span>🎞</span>}</div>
        <div>
          <h1>{m.title}</h1>
          {m.originalTitle && <div className="muted">{m.originalTitle}</div>}
          <p><StatusBadge value={m.status} /> {m.ageRating && <span className="badge">{m.ageRating}</span>}</p>
          <dl className="info">
            <dt>Thời lượng</dt><dd>{m.durationMinutes} phút</dd>
            <dt>Khởi chiếu</dt><dd>{fmtDate(m.releaseDate)}</dd>
            <dt>Thể loại</dt><dd>{m.genres.map((g) => g.name).join(', ') || '—'}</dd>
            <dt>Đạo diễn</dt><dd>{m.directors.map((d) => d.name).join(', ') || '—'}</dd>
            <dt>Diễn viên</dt><dd>{m.cast.map((c) => c.name + (c.characterName ? ` (${c.characterName})` : '')).join(', ') || '—'}</dd>
            <dt>Ngôn ngữ / Quốc gia</dt><dd>{m.language ?? '—'} / {m.country ?? '—'}</dd>
          </dl>
          {m.description && <p>{m.description}</p>}
        </div>
      </div>
      <h2>Lịch chiếu</h2>
      {m.status === 'NOW_SHOWING' ? <ShowtimeBrowser movieId={m.id} groupBy="cinema" /> : <p className="muted">Phim chưa / không còn chiếu.</p>}
    </>
  );
}
