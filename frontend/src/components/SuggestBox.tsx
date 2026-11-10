import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchApi } from '../api/endpoints';
import type { Suggestion } from '../api/types';

/** Search input with GET /api/v1/search/movies/suggest while typing */
export function SuggestBox({ initial = '', onSubmit }: { initial?: string; onSubmit: (q: string) => void }) {
  const [q, setQ] = useState(initial);
  const [items, setItems] = useState<Suggestion[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (q.trim().length < 2) { setItems([]); return; }
    const t = setTimeout(() => {
      searchApi.suggest(q.trim()).then(setItems).catch(() => setItems([]));
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  return (
    <form className="suggest" onSubmit={(e) => { e.preventDefault(); setOpen(false); onSubmit(q.trim()); }}>
      <input value={q} placeholder="Tìm phim, đạo diễn, diễn viên…" onChange={(e) => { setQ(e.target.value); setOpen(true); }}
        onBlur={() => setTimeout(() => setOpen(false), 150)} />
      <button type="submit">Tìm</button>
      {open && items.length > 0 && (
        <ul className="suggest-list">
          {items.map((s) => (
            <li key={s.id}>
              <Link to={`/movies/${s.id}`}>{s.title} {s.releaseDate && <span className="muted small">({s.releaseDate.slice(0, 4)})</span>}</Link>
            </li>
          ))}
        </ul>
      )}
    </form>
  );
}
