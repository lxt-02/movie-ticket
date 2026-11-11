import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, X, Clock, Zap, Database, ChevronLeft, ChevronRight, User } from 'lucide-react';
import { searchMoviesES, suggestMovies } from '../api';

/**
 * Màn tìm kiếm phim dùng search-service (Elasticsearch read model, đồng bộ từ movie-service qua Kafka).
 * - Full-text: title^4, originalTitle^3, directors^2, actors^2, characters, description
 * - Facet (aggregation) có đếm số lượng: genres, status, ageRating, language, country
 * - Highlight từ khoá (<em>), sort, lọc theo thời lượng / ngày phát hành, autocomplete /suggest
 * Trạng thái bộ lọc nằm trên URL để copy link / F5 vẫn giữ nguyên.
 */

const FACET_LABELS = {
  genres: 'Thể loại',
  status: 'Trạng thái',
  ageRating: 'Độ tuổi',
  language: 'Ngôn ngữ',
  country: 'Quốc gia',
};
const STATUS_LABELS = {
  NOW_SHOWING: 'Đang chiếu',
  COMING_SOON: 'Sắp chiếu',
  ENDED: 'Đã kết thúc',
};
const SORTS = [
  ['relevance', 'Liên quan nhất'],
  ['newest', 'Mới phát hành'],
  ['oldest', 'Cũ nhất'],
  ['title', 'Tên A → Z'],
  ['duration_asc', 'Thời lượng ngắn → dài'],
  ['duration_desc', 'Thời lượng dài → ngắn'],
];
const LIST_KEYS = ['genres', 'status', 'ageRating', 'language', 'country'];
const SCALAR_KEYS = ['q', 'person', 'sort', 'releaseFrom', 'releaseTo', 'durationMin', 'durationMax', 'page'];

/** Chỉ cho phép thẻ <em> của ES highlight, mọi thứ khác bị escape. */
function safeHighlight(fragment) {
  const escaped = String(fragment)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
  return escaped
    .replace(/&lt;em&gt;/g, '<mark class="bg-amber-200 text-slate-900 rounded px-0.5">')
    .replace(/&lt;\/em&gt;/g, '</mark>');
}

function Highlighted({ html, fallback, className }) {
  if (!html) return <span className={className}>{fallback}</span>;
  return <span className={className} dangerouslySetInnerHTML={{ __html: safeHighlight(html) }} />;
}

function readParams(sp) {
  const state = {};
  LIST_KEYS.forEach((k) => (state[k] = sp.getAll(k)));
  SCALAR_KEYS.forEach((k) => (state[k] = sp.get(k) || ''));
  return state;
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const filters = useMemo(() => readParams(searchParams), [searchParams]);

  const [keyword, setKeyword] = useState(filters.q);
  const [person, setPerson] = useState(filters.person);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggest, setShowSuggest] = useState(false);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const suggestTimer = useRef(null);

  // URL đổi (vd. từ ô search trên Navbar) -> đồng bộ lại ô nhập
  useEffect(() => {
    setKeyword(filters.q);
    setPerson(filters.person);
  }, [filters.q, filters.person]);

  // Gọi Elasticsearch mỗi khi bộ lọc trên URL thay đổi
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    searchMoviesES({
      ...filters,
      page: Number(filters.page || 0),
      size: 12,
    })
      .then((data) => !cancelled && setResult(data))
      .catch((e) => !cancelled && setError(e.message))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [filters]);

  const update = (changes, { resetPage = true } = {}) => {
    const next = { ...filters, ...changes };
    if (resetPage) next.page = '';
    const sp = new URLSearchParams();
    LIST_KEYS.forEach((k) => (next[k] || []).forEach((v) => sp.append(k, v)));
    SCALAR_KEYS.forEach((k) => next[k] !== '' && next[k] != null && sp.set(k, next[k]));
    setSearchParams(sp);
  };

  const toggleFacet = (facet, value) => {
    const current = filters[facet] || [];
    update({ [facet]: current.includes(value) ? current.filter((v) => v !== value) : [...current, value] });
  };

  const onKeywordChange = (value) => {
    setKeyword(value);
    clearTimeout(suggestTimer.current);
    if (!value.trim()) {
      setSuggestions([]);
      return;
    }
    suggestTimer.current = setTimeout(() => {
      suggestMovies(value)
        .then((list) => {
          setSuggestions(list || []);
          setShowSuggest(true);
        })
        .catch(() => setSuggestions([]));
    }, 250);
  };

  const submit = (e) => {
    e.preventDefault();
    setShowSuggest(false);
    update({ q: keyword.trim(), person: person.trim() });
  };

  const clearAll = () => {
    setKeyword('');
    setPerson('');
    setSearchParams(new URLSearchParams());
  };

  const activeChips = [
    ...LIST_KEYS.flatMap((k) => (filters[k] || []).map((v) => ({ key: k, value: v }))),
  ];
  const hasAnyFilter =
    activeChips.length > 0 || filters.q || filters.person || filters.releaseFrom || filters.releaseTo ||
    filters.durationMin || filters.durationMax;

  const page = result?.page ?? 0;
  const totalPages = result?.totalPages ?? 0;

  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 space-y-6">
      {/* Header + search box */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-black text-slate-900 uppercase tracking-tight">Tìm Kiếm Phim</h1>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5">
              <Database className="w-3.5 h-3.5 text-amber-600" />
              Tìm trên Elasticsearch (search-service) — dữ liệu đồng bộ từ movie-service qua Kafka
            </p>
          </div>
          {result && (
            <div className="flex items-center gap-2 text-xs">
              <span className="px-3 py-1.5 rounded-full bg-slate-100 font-bold text-slate-700">
                {result.totalElements} kết quả
              </span>
              <span className="px-3 py-1.5 rounded-full bg-amber-50 border border-amber-200 font-bold text-amber-700 flex items-center gap-1">
                <Zap className="w-3.5 h-3.5" /> ES took {result.tookMs} ms
              </span>
            </div>
          )}
        </div>

        <form onSubmit={submit} className="grid grid-cols-1 md:grid-cols-[1fr_260px_auto] gap-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              value={keyword}
              onChange={(e) => onKeywordChange(e.target.value)}
              onFocus={() => suggestions.length && setShowSuggest(true)}
              onBlur={() => setTimeout(() => setShowSuggest(false), 150)}
              placeholder="Tên phim, tên gốc, nội dung... (thử gõ sai chính tả)"
              className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-9 pr-3 py-3 text-sm focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500/30"
            />
            {showSuggest && suggestions.length > 0 && (
              <div className="absolute z-30 mt-1 w-full bg-white border border-slate-200 rounded-2xl shadow-xl overflow-hidden">
                <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 bg-slate-50">
                  Gợi ý (/search/movies/suggest)
                </div>
                {suggestions.map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onMouseDown={() => navigate(`/movies/${s.id}`)}
                    className="w-full flex items-center gap-3 px-3 py-2 hover:bg-red-50 text-left"
                  >
                    <img
                      src={s.posterUrl || 'https://placehold.co/60x90/1e293b/f8fafc?text=MTK'}
                      alt=""
                      className="w-8 h-12 object-cover rounded"
                    />
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-slate-800 truncate">{s.title}</div>
                      <div className="text-[11px] text-slate-500 truncate">
                        {s.originalTitle} {s.releaseDate ? `• ${s.releaseDate.slice(0, 4)}` : ''}{' '}
                        {s.status ? `• ${STATUS_LABELS[s.status] || s.status}` : ''}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="relative">
            <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              value={person}
              onChange={(e) => setPerson(e.target.value)}
              placeholder="Đạo diễn / diễn viên"
              className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-9 pr-3 py-3 text-sm focus:outline-none focus:border-red-500"
            />
          </div>
          <div className="flex gap-2">
            <button className="px-6 py-3 rounded-2xl bg-red-600 hover:bg-red-500 text-white font-black text-xs uppercase tracking-wider">
              Tìm
            </button>
            <button
              type="button"
              onClick={() => setShowFilters((v) => !v)}
              className="lg:hidden px-4 py-3 rounded-2xl border border-slate-200 text-slate-700"
            >
              <SlidersHorizontal className="w-4 h-4" />
            </button>
          </div>
        </form>

        {hasAnyFilter && (
          <div className="flex flex-wrap items-center gap-2 text-xs">
            {filters.q && <Chip label={`"${filters.q}"`} onRemove={() => update({ q: '' })} />}
            {filters.person && <Chip label={`Người: ${filters.person}`} onRemove={() => update({ person: '' })} />}
            {activeChips.map(({ key, value }) => (
              <Chip
                key={key + value}
                label={`${FACET_LABELS[key]}: ${key === 'status' ? STATUS_LABELS[value] || value : value}`}
                onRemove={() => toggleFacet(key, value)}
              />
            ))}
            {(filters.durationMin || filters.durationMax) && (
              <Chip
                label={`Thời lượng ${filters.durationMin || 0}–${filters.durationMax || '∞'} phút`}
                onRemove={() => update({ durationMin: '', durationMax: '' })}
              />
            )}
            {(filters.releaseFrom || filters.releaseTo) && (
              <Chip
                label={`Phát hành ${filters.releaseFrom || '…'} → ${filters.releaseTo || '…'}`}
                onRemove={() => update({ releaseFrom: '', releaseTo: '' })}
              />
            )}
            <button onClick={clearAll} className="text-red-600 font-bold hover:underline ml-1">
              Xoá tất cả
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        {/* Facets */}
        <aside className={`${showFilters ? 'block' : 'hidden'} lg:block space-y-4`}>
          <div className="bg-white border border-slate-200 rounded-3xl p-5 shadow-sm space-y-2">
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-700">Sắp xếp</h3>
            <select
              value={filters.sort || 'relevance'}
              onChange={(e) => update({ sort: e.target.value === 'relevance' ? '' : e.target.value })}
              className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm"
            >
              {SORTS.map(([v, label]) => (
                <option key={v} value={v}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          {Object.keys(FACET_LABELS).map((facet) => {
            const buckets = result?.facets?.[facet] || [];
            const selected = filters[facet] || [];
            if (!buckets.length && !selected.length) return null;
            return (
              <div key={facet} className="bg-white border border-slate-200 rounded-3xl p-5 shadow-sm">
                <h3 className="text-xs font-black uppercase tracking-wider text-slate-700 mb-3">
                  {FACET_LABELS[facet]}
                </h3>
                <div className="space-y-1.5 max-h-60 overflow-y-auto">
                  {buckets.map((b) => (
                    <label key={b.value} className="flex items-center justify-between gap-2 text-sm cursor-pointer group">
                      <span className="flex items-center gap-2 min-w-0">
                        <input
                          type="checkbox"
                          checked={selected.includes(b.value)}
                          onChange={() => toggleFacet(facet, b.value)}
                          className="accent-red-600"
                        />
                        <span className="truncate text-slate-700 group-hover:text-red-600">
                          {facet === 'status' ? STATUS_LABELS[b.value] || b.value : b.value}
                        </span>
                      </span>
                      <span className="text-[11px] font-bold text-slate-400 bg-slate-100 rounded-full px-2">{b.count}</span>
                    </label>
                  ))}
                </div>
              </div>
            );
          })}

          <RangeFilters filters={filters} onApply={update} />
        </aside>

        {/* Results */}
        <section className="space-y-4">
          {error && (
            <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
              <div className="text-xs mt-1 text-red-500">
                Kiểm tra search-service và Elasticsearch đã chạy; nếu index trống, ADMIN gọi POST /api/v1/movies/search-sync.
              </div>
            </div>
          )}

          {loading && !result && (
            <div className="py-20 flex justify-center">
              <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {result && result.items.length === 0 && !loading && (
            <div className="p-10 text-center bg-white border border-slate-200 rounded-3xl text-sm text-slate-500">
              Không tìm thấy phim phù hợp. Thử bỏ bớt bộ lọc hoặc đổi từ khoá.
            </div>
          )}

          <div className={`grid grid-cols-1 md:grid-cols-2 gap-4 ${loading ? 'opacity-60' : ''}`}>
            {result?.items.map((m) => (
              <Link
                key={m.id}
                to={`/movies/${m.id}`}
                className="flex gap-4 bg-white border border-slate-200 rounded-3xl p-4 shadow-sm hover:shadow-lg hover:border-red-300 transition-all"
              >
                <img src={m.posterUrl} alt={m.title} className="w-24 h-36 object-cover rounded-2xl shrink-0 bg-slate-100" />
                <div className="min-w-0 flex-1 space-y-1.5">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-black text-slate-900 uppercase leading-tight">
                      <Highlighted html={m.highlight?.title?.[0]} fallback={m.title} />
                    </h3>
                    {m.ageRating && (
                      <span className="shrink-0 px-2 py-0.5 rounded-md bg-red-600 text-white text-[10px] font-black">
                        {m.ageRating}
                      </span>
                    )}
                  </div>
                  {m.originalTitle && (
                    <p className="text-xs italic text-slate-500">
                      <Highlighted html={m.highlight?.originalTitle?.[0]} fallback={m.originalTitle} />
                    </p>
                  )}
                  <div className="flex flex-wrap gap-1.5 text-[11px]">
                    {m.status && (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-bold">
                        {STATUS_LABELS[m.status] || m.status}
                      </span>
                    )}
                    {m.genres.map((g) => (
                      <span key={g} className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                        {g}
                      </span>
                    ))}
                    {m.durationMinutes ? (
                      <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 flex items-center gap-1">
                        <Clock className="w-3 h-3" /> {m.durationMinutes}′
                      </span>
                    ) : null}
                  </div>
                  {(m.directorList.length > 0 || m.actorList.length > 0) && (
                    <p className="text-[11px] text-slate-600 line-clamp-2">
                      {m.directorList.length > 0 && (
                        <>
                          <strong>ĐD:</strong>{' '}
                          <Highlighted html={m.highlight?.directors?.join(', ')} fallback={m.director} />{' '}
                        </>
                      )}
                      {m.actorList.length > 0 && (
                        <>
                          <strong>DV:</strong>{' '}
                          <Highlighted html={m.highlight?.actors?.join(', ')} fallback={m.actors} />
                        </>
                      )}
                    </p>
                  )}
                  <p className="text-xs text-slate-500 line-clamp-3">
                    <Highlighted html={m.highlight?.description?.[0]} fallback={m.synopsis} />
                  </p>
                  {filters.q && m.score != null && (
                    <p className="text-[10px] font-mono text-slate-400">_score: {m.score.toFixed(2)}</p>
                  )}
                </div>
              </Link>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                disabled={page <= 0}
                onClick={() => update({ page: String(page - 1) }, { resetPage: false })}
                className="p-2 rounded-xl border border-slate-200 bg-white disabled:opacity-40"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <span className="text-sm font-bold text-slate-700">
                Trang {page + 1} / {totalPages}
              </span>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => update({ page: String(page + 1) }, { resetPage: false })}
                className="p-2 rounded-xl border border-slate-200 bg-white disabled:opacity-40"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Chip({ label, onRemove }) {
  return (
    <span className="inline-flex items-center gap-1 pl-3 pr-1.5 py-1 rounded-full bg-red-50 border border-red-200 text-red-700 font-bold">
      {label}
      <button type="button" onClick={onRemove} className="p-0.5 rounded-full hover:bg-red-100">
        <X className="w-3 h-3" />
      </button>
    </span>
  );
}

function RangeFilters({ filters, onApply }) {
  const [form, setForm] = useState(filters);
  useEffect(() => setForm(filters), [filters]);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  return (
    <div className="bg-white border border-slate-200 rounded-3xl p-5 shadow-sm space-y-3 text-sm">
      <h3 className="text-xs font-black uppercase tracking-wider text-slate-700">Thời lượng (phút)</h3>
      <div className="flex gap-2">
        <input type="number" min="0" value={form.durationMin} onChange={set('durationMin')} placeholder="Từ"
          className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2" />
        <input type="number" min="0" value={form.durationMax} onChange={set('durationMax')} placeholder="Đến"
          className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2" />
      </div>
      <h3 className="text-xs font-black uppercase tracking-wider text-slate-700 pt-1">Ngày phát hành</h3>
      <input type="date" value={form.releaseFrom} onChange={set('releaseFrom')}
        className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2" />
      <input type="date" value={form.releaseTo} onChange={set('releaseTo')}
        className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3 py-2" />
      <button
        type="button"
        onClick={() =>
          onApply({
            durationMin: form.durationMin,
            durationMax: form.durationMax,
            releaseFrom: form.releaseFrom,
            releaseTo: form.releaseTo,
          })
        }
        className="w-full py-2 rounded-xl bg-slate-900 hover:bg-slate-700 text-white font-bold text-xs uppercase"
      >
        Áp dụng
      </button>
    </div>
  );
}
