import { useState } from 'react';
import { moviesApi, searchApi } from '../../api/endpoints';
import { ErrorBox, FieldError, useAction, useAsync } from '../../components/ui';

/** ADMIN: genres, persons, search index (POST /movies/genres, /movies/persons, /movies/search-sync, /search/admin/movies/recreate-index) */
export default function AdminCatalog() {
  const genres = useAsync(() => moviesApi.genres(), []);
  const [q, setQ] = useState('');
  const persons = useAsync(() => moviesApi.persons({ q, size: 20 }), [q]);
  const [genre, setGenre] = useState('');
  const [person, setPerson] = useState({ name: '', avatarUrl: '', biography: '' });
  const [msg, setMsg] = useState<string>();
  const g = useAction();
  const p = useAction();
  const s = useAction();

  return (
    <>
      <h1>Thể loại · Đoàn phim · Tìm kiếm</h1>
      <div className="grid-2">
        <div className="card">
          <h3>Thể loại</h3>
          <div className="row wrap">{genres.data?.map((x) => <span key={x.id} className="chip">{x.name}</span>)}</div>
          <form className="row" onSubmit={(e) => { e.preventDefault(); g.run(async () => { await moviesApi.createGenre(genre); setGenre(''); await genres.reload(); }); }}>
            <input placeholder="Tên thể loại mới" required value={genre} onChange={(e) => setGenre(e.target.value)} />
            <button disabled={g.busy}>Thêm</button>
          </form>
          <FieldError error={g.error} field="name" />
          <ErrorBox error={g.error} />
        </div>

        <div className="card">
          <h3>Người (đạo diễn / diễn viên)</h3>
          <input placeholder="Tìm…" value={q} onChange={(e) => setQ(e.target.value)} />
          <ul className="plain small">{persons.data?.items.map((x) => <li key={x.id}>{x.name}</li>)}</ul>
          <form className="form" onSubmit={(e) => {
            e.preventDefault();
            p.run(async () => {
              await moviesApi.createPerson({ name: person.name, avatarUrl: person.avatarUrl || undefined, biography: person.biography || undefined });
              setPerson({ name: '', avatarUrl: '', biography: '' });
              await persons.reload();
            });
          }}>
            <input placeholder="Tên *" required value={person.name} onChange={(e) => setPerson({ ...person, name: e.target.value })} />
            <input placeholder="Ảnh (URL)" value={person.avatarUrl} onChange={(e) => setPerson({ ...person, avatarUrl: e.target.value })} />
            <textarea placeholder="Tiểu sử" rows={2} value={person.biography} onChange={(e) => setPerson({ ...person, biography: e.target.value })} />
            <button disabled={p.busy}>Thêm người</button>
          </form>
          <ErrorBox error={p.error} />
        </div>

        <div className="card">
          <h3>Chỉ mục tìm kiếm (Elasticsearch)</h3>
          <p className="small muted">Tạo lại index rỗng rồi đẩy lại toàn bộ phim từ movie-service.</p>
          <div className="row">
            <button disabled={s.busy} onClick={() => s.run(async () => { await movieSync(); setMsg('Đã gửi lại toàn bộ phim sang search-service'); })}>Đồng bộ lại phim</button>
            <button className="danger" disabled={s.busy} onClick={() => s.run(async () => {
              if (!confirm('Xoá và tạo lại index? Tìm kiếm sẽ rỗng tới khi đồng bộ xong.')) return;
              await searchApi.recreateIndex();
              await movieSync();
              setMsg('Đã tạo lại index và đồng bộ phim');
            })}>Tạo lại index + đồng bộ</button>
          </div>
          {msg && <p className="ok small">{msg}</p>}
          <ErrorBox error={s.error} />
        </div>
      </div>
    </>
  );
}

const movieSync = () => moviesApi.searchSync();
