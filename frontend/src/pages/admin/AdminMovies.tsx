import { useState } from 'react';
import { Link } from 'react-router-dom';
import { moviesApi } from '../../api/endpoints';
import type { Movie, MovieStatus } from '../../api/types';
import { ErrorBox, Loading, Pagination, StatusBadge, useAction, useAsync } from '../../components/ui';
import { fmtDate, label } from '../../utils/format';

/** Allowed status transitions (NGHIEP_VU §3.1) */
const NEXT: Record<MovieStatus, MovieStatus[]> = {
  COMING_SOON: ['NOW_SHOWING'],
  NOW_SHOWING: ['ENDED'],
  ENDED: ['NOW_SHOWING'],
  ARCHIVED: [],
};

/** ADMIN: GET /movies (incl. ARCHIVED), PATCH /movies/{id}/status, DELETE /movies/{id} (archive) */
export default function AdminMovies() {
  const [status, setStatus] = useState<MovieStatus | ''>('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const list = useAsync(() => moviesApi.list({ status: status || undefined, q, page, size: 20 }), [status, q, page]);
  const action = useAction();

  const replace = (m: Movie) =>
    list.setData((d) => d && { ...d, items: d.items.map((x) => (x.id === m.id ? m : x)) });

  return (
    <>
      <div className="row space">
        <h1>Quản lý phim</h1>
        <Link to="/admin/movies/new" className="button primary">+ Thêm phim</Link>
      </div>
      <div className="row">
        <input placeholder="Tìm tên phim…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <select value={status} onChange={(e) => { setStatus(e.target.value as MovieStatus | ''); setPage(0); }}>
          <option value="">Mọi trạng thái</option>
          {(['COMING_SOON', 'NOW_SHOWING', 'ENDED', 'ARCHIVED'] as const).map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      <ErrorBox error={list.error ?? action.error} />
      {list.loading ? <Loading /> : (
        <table className="table">
          <thead><tr><th>Tên</th><th>Thời lượng</th><th>Khởi chiếu</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            {list.data?.items.map((m) => (
              <tr key={m.id}>
                <td><Link to={`/movies/${m.id}`}>{m.title}</Link></td>
                <td>{m.durationMinutes}′</td>
                <td>{fmtDate(m.releaseDate)}</td>
                <td><StatusBadge value={m.status} /></td>
                <td className="row">
                  {m.status !== 'ARCHIVED' && <Link to={`/admin/movies/${m.id}`}>Sửa</Link>}
                  {NEXT[m.status].map((s) => (
                    <button key={s} className="small" disabled={action.busy}
                      onClick={() => action.run(async () => replace(await moviesApi.setStatus(m.id, s)))}>
                      → {label(s)}
                    </button>
                  ))}
                  {m.status !== 'ARCHIVED' && (
                    <button className="small danger" disabled={action.busy} onClick={() => action.run(async () => {
                      if (!confirm(`Lưu trữ phim "${m.title}"? Không thể sửa lại sau đó.`)) return;
                      await moviesApi.archive(m.id);
                      await list.reload();
                    })}>Lưu trữ</button>
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
