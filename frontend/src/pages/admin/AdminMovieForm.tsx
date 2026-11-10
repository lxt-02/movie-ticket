import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { moviesApi } from '../../api/endpoints';
import type { AgeRating, MovieRequest, Person } from '../../api/types';
import { ErrorBox, FieldError, Loading, useAction, useAsync } from '../../components/ui';

type CrewRow = { personId: string; name: string; role: 'DIRECTOR' | 'ACTOR'; characterName?: string };

const EMPTY: MovieRequest = { title: '', durationMinutes: 120, genreIds: [], crew: [], status: 'COMING_SOON' };

/** ADMIN: POST /movies, PUT /movies/{id} (MovieRequest, FRONTEND_API §4.2) */
export default function AdminMovieForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const genres = useAsync(() => moviesApi.genres(), []);
  const [form, setForm] = useState<MovieRequest>(EMPTY);
  const [crew, setCrew] = useState<CrewRow[]>([]);
  const [loaded, setLoaded] = useState(!id);
  const action = useAction();

  useEffect(() => {
    if (!id) return;
    moviesApi.get(id).then((m) => {
      setForm({
        title: m.title, originalTitle: m.originalTitle ?? '', description: m.description ?? '',
        durationMinutes: m.durationMinutes, releaseDate: m.releaseDate ?? '', ageRating: m.ageRating ?? undefined,
        language: m.language ?? '', country: m.country ?? '', posterUrl: m.posterUrl ?? '', status: m.status,
        genreIds: m.genres.map((g) => g.id), crew: [],
      });
      setCrew([
        ...m.directors.map((d) => ({ personId: d.id, name: d.name, role: 'DIRECTOR' as const })),
        ...m.cast.map((c) => ({ personId: c.id, name: c.name, role: 'ACTOR' as const, characterName: c.characterName ?? '' })),
      ]);
      setLoaded(true);
    }).catch(action.setError);
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const set = <K extends keyof MovieRequest>(k: K, v: MovieRequest[K]) => setForm((f) => ({ ...f, [k]: v }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const body: MovieRequest = {
      ...form,
      // send undefined instead of empty strings
      originalTitle: form.originalTitle || undefined,
      description: form.description || undefined,
      releaseDate: form.releaseDate || undefined,
      language: form.language || undefined,
      country: form.country || undefined,
      posterUrl: form.posterUrl || undefined,
      crew: crew.map(({ personId, role, characterName }) => ({ personId, role, characterName: characterName || undefined })),
    };
    action.run(async () => {
      if (id) await moviesApi.update(id, body);
      else await moviesApi.create(body);
      navigate('/admin/movies');
    });
  };

  if (!loaded) return <Loading />;

  return (
    <form className="card form" onSubmit={submit}>
      <h1>{id ? 'Sửa phim' : 'Thêm phim'}</h1>
      <div className="grid-2">
        <label>Tên phim *<input required maxLength={255} value={form.title} onChange={(e) => set('title', e.target.value)} />
          <FieldError error={action.error} field="title" /></label>
        <label>Tên gốc<input value={form.originalTitle ?? ''} onChange={(e) => set('originalTitle', e.target.value)} /></label>
        <label>Thời lượng (phút) *<input type="number" min={1} max={600} required value={form.durationMinutes}
          onChange={(e) => set('durationMinutes', Number(e.target.value))} />
          <FieldError error={action.error} field="durationMinutes" /></label>
        <label>Ngày phát hành<input type="date" value={form.releaseDate ?? ''} onChange={(e) => set('releaseDate', e.target.value)} /></label>
        <label>Độ tuổi
          <select value={form.ageRating ?? ''} onChange={(e) => set('ageRating', (e.target.value || undefined) as AgeRating)}>
            <option value="">—</option>
            {['P', 'K', 'T13', 'T16', 'T18'].map((a) => <option key={a}>{a}</option>)}
          </select>
        </label>
        {!id && (
          <label>Trạng thái ban đầu
            <select value={form.status} onChange={(e) => set('status', e.target.value as MovieRequest['status'])}>
              <option value="COMING_SOON">Sắp chiếu</option>
              <option value="NOW_SHOWING">Đang chiếu</option>
            </select>
          </label>
        )}
        <label>Ngôn ngữ<input value={form.language ?? ''} onChange={(e) => set('language', e.target.value)} /></label>
        <label>Quốc gia<input value={form.country ?? ''} onChange={(e) => set('country', e.target.value)} /></label>
      </div>
      <label>Poster URL<input value={form.posterUrl ?? ''} onChange={(e) => set('posterUrl', e.target.value)} /></label>
      <label>Mô tả<textarea rows={4} value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} /></label>

      <fieldset>
        <legend>Thể loại</legend>
        {genres.data?.map((g) => (
          <label key={g.id} className="check inline">
            <input type="checkbox" checked={form.genreIds.includes(g.id)}
              onChange={() => set('genreIds', form.genreIds.includes(g.id) ? form.genreIds.filter((x) => x !== g.id) : [...form.genreIds, g.id])} />
            {g.name}
          </label>
        ))}
        <FieldError error={action.error} field="genreIds" />
      </fieldset>

      <fieldset>
        <legend>Đoàn phim (tối đa 50)</legend>
        {crew.map((c, i) => (
          <div key={c.personId + c.role} className="row">
            <span className="grow">{c.name}</span>
            <select value={c.role} onChange={(e) => setCrew(crew.map((x, k) => k === i ? { ...x, role: e.target.value as CrewRow['role'] } : x))}>
              <option value="DIRECTOR">Đạo diễn</option>
              <option value="ACTOR">Diễn viên</option>
            </select>
            {c.role === 'ACTOR' && <input placeholder="Tên nhân vật" value={c.characterName ?? ''}
              onChange={(e) => setCrew(crew.map((x, k) => k === i ? { ...x, characterName: e.target.value } : x))} />}
            <button type="button" className="ghost" onClick={() => setCrew(crew.filter((_, k) => k !== i))}>✕</button>
          </div>
        ))}
        {crew.length < 50 && <PersonPicker onPick={(p) => !crew.some((c) => c.personId === p.id) && setCrew([...crew, { personId: p.id, name: p.name, role: 'ACTOR' }])} />}
        <FieldError error={action.error} field="crew" />
      </fieldset>

      <ErrorBox error={action.error} />
      <div className="row">
        <button className="primary" disabled={action.busy}>{id ? 'Lưu thay đổi' : 'Tạo phim'}</button>
        <button type="button" className="ghost" onClick={() => navigate(-1)}>Huỷ</button>
      </div>
    </form>
  );
}

function PersonPicker({ onPick }: { onPick: (p: Person) => void }) {
  const [q, setQ] = useState('');
  const [items, setItems] = useState<Person[]>([]);
  useEffect(() => {
    if (q.trim().length < 1) { setItems([]); return; }
    const t = setTimeout(() => moviesApi.persons({ q, size: 8 }).then((p) => setItems(p.items)).catch(() => setItems([])), 250);
    return () => clearTimeout(t);
  }, [q]);
  return (
    <div className="picker">
      <input placeholder="Tìm người để thêm (đạo diễn / diễn viên)…" value={q} onChange={(e) => setQ(e.target.value)} />
      {items.length > 0 && (
        <ul className="suggest-list static">
          {items.map((p) => (
            <li key={p.id}><button type="button" className="link" onClick={() => { onPick(p); setQ(''); }}>{p.name}</button></li>
          ))}
        </ul>
      )}
    </div>
  );
}
