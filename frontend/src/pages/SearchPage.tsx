import { useSearchParams } from 'react-router-dom';
import { searchApi, type SearchQuery } from '../api/endpoints';
import { ErrorBox, Loading, Pagination, useAsync } from '../components/ui';
import { MovieCard } from '../components/MovieCard';
import { SuggestBox } from '../components/SuggestBox';
import { label, safeHighlight } from '../utils/format';

const FACET_KEYS = ['genres', 'status', 'ageRating', 'language', 'country'] as const;
const FACET_TITLES: Record<string, string> = {
  genres: 'Thể loại', status: 'Trạng thái', ageRating: 'Độ tuổi', language: 'Ngôn ngữ', country: 'Quốc gia',
};
const SORTS: { value: NonNullable<SearchQuery['sort']>; text: string }[] = [
  { value: 'RELEVANCE', text: 'Liên quan nhất' },
  { value: 'NEWEST', text: 'Mới nhất' },
  { value: 'OLDEST', text: 'Cũ nhất' },
  { value: 'TITLE', text: 'Tên A–Z' },
  { value: 'DURATION_ASC', text: 'Ngắn → dài' },
  { value: 'DURATION_DESC', text: 'Dài → ngắn' },
];

/** GET /api/v1/search/movies (public, Elasticsearch) with facets */
export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const sort = (params.get('sort') as SearchQuery['sort']) ?? 'RELEVANCE';
  const page = Number(params.get('page') ?? 0);
  const filters = Object.fromEntries(FACET_KEYS.map((k) => [k, params.getAll(k)])) as Record<(typeof FACET_KEYS)[number], string[]>;

  const { data, error, loading } = useAsync(
    () => searchApi.movies({ q: q || undefined, sort, page, size: 12, ...filters }),
    [params.toString()],
  );

  const update = (fn: (p: URLSearchParams) => void) => {
    const next = new URLSearchParams(params);
    fn(next);
    next.delete('page');
    setParams(next);
  };
  const toggle = (key: string, value: string) =>
    update((p) => {
      const values = p.getAll(key);
      p.delete(key);
      (values.includes(value) ? values.filter((v) => v !== value) : [...values, value]).forEach((v) => p.append(key, v));
    });

  return (
    <div className="search-layout">
      <aside className="card facets">
        <h3>Bộ lọc</h3>
        {FACET_KEYS.map((key) => {
          const buckets = data?.facets?.[key] ?? [];
          if (buckets.length === 0 && filters[key].length === 0) return null;
          return (
            <div key={key} className="facet">
              <div className="facet-title">{FACET_TITLES[key]}</div>
              {buckets.map((b) => (
                <label key={b.value} className="check">
                  <input type="checkbox" checked={filters[key].includes(b.value)} onChange={() => toggle(key, b.value)} />
                  {key === 'status' ? label(b.value) : b.value} <span className="muted">({b.count})</span>
                </label>
              ))}
            </div>
          );
        })}
      </aside>
      <section>
        <div className="row">
          <SuggestBox key={q} initial={q} onSubmit={(v) => update((p) => (v ? p.set('q', v) : p.delete('q')))} />
          <select value={sort} onChange={(e) => update((p) => p.set('sort', e.target.value))}>
            {SORTS.map((s) => <option key={s.value} value={s.value}>{s.text}</option>)}
          </select>
        </div>
        <ErrorBox error={error} />
        {loading ? <Loading /> : data && (
          <>
            <p className="muted small">{data.totalElements} kết quả · {data.tookMs} ms</p>
            <div className="movie-grid">
              {data.items.map((h) => {
                const frag = h.highlight && Object.values(h.highlight)[0]?.[0];
                return (
                  <MovieCard key={h.id} id={h.id} title={h.title} posterUrl={h.posterUrl} status={h.status}
                    meta={[h.durationMinutes && `${h.durationMinutes} phút`, h.ageRating, h.genres?.join(', ')].filter(Boolean).join(' · ')}
                    highlightHtml={frag ? safeHighlight(frag) : undefined} />
                );
              })}
            </div>
            <Pagination page={data.page} totalPages={data.totalPages}
              onChange={(p) => { const n = new URLSearchParams(params); n.set('page', String(p)); setParams(n); }} />
          </>
        )}
      </section>
    </div>
  );
}
