import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cinemasApi } from '../api/endpoints';
import { ErrorBox, Loading, Pagination, useAsync } from '../components/ui';

/** GET /api/v1/cinemas (public) */
export default function CinemasPage() {
  const [city, setCity] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const { data, error, loading } = useAsync(() => cinemasApi.list({ city, q, status: 'ACTIVE', page, size: 20 }), [city, q, page]);

  return (
    <>
      <h1>Hệ thống rạp</h1>
      <div className="row">
        <input placeholder="Tên rạp…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <input placeholder="Thành phố…" value={city} onChange={(e) => { setCity(e.target.value); setPage(0); }} />
      </div>
      <ErrorBox error={error} />
      {loading ? <Loading /> : (
        <div className="grid-2">
          {data?.items.map((c) => (
            <Link key={c.id} to={`/cinemas/${c.id}`} className="card link-card">
              <h3>{c.name}</h3>
              <div className="muted">{c.brand}</div>
              <div>{c.address}{c.district ? `, ${c.district}` : ''}, {c.city}</div>
              {c.phone && <div className="small">☎ {c.phone}</div>}
            </Link>
          ))}
        </div>
      )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  );
}
