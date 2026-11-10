import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { cinemasApi, moviesApi, showtimesApi } from '../../api/endpoints';
import type { Screen, Showtime, ShowtimeStatus } from '../../api/types';
import { ErrorBox, FieldError, Loading, Modal, Pagination, StatusBadge, useAction, useAsync } from '../../components/ui';
import { fmtDateTime, fmtMoney, isoToVnLocal, label, vnDate, vnLocalToIso } from '../../utils/format';

const STATUSES: ShowtimeStatus[] = ['SCHEDULED', 'SELLING', 'ENDED', 'CANCELLED'];

/**
 * ADMIN: GET /showtimes (admin also sees past + any status), POST /showtimes, PUT /showtimes/{id}, DELETE (cancel).
 * Rules: movie NOW_SHOWING, cinema & screen ACTIVE, ≥15 min gap in the same screen, no edit/cancel once seats are held/sold.
 */
export default function AdminShowtimes() {
  const movies = useAsync(() => moviesApi.list({ status: 'NOW_SHOWING', size: 100 }), []);
  const cinemas = useAsync(() => cinemasApi.list({ status: 'ACTIVE', size: 100 }), []);
  const [filter, setFilter] = useState({ movieId: '', cinemaId: '', date: vnDate(0), status: [] as ShowtimeStatus[] });
  const [page, setPage] = useState(0);
  const list = useAsync(
    () => showtimesApi.list({ movieId: filter.movieId || undefined, cinemaId: filter.cinemaId || undefined, date: filter.date || undefined, status: filter.status, page, size: 20 }),
    [JSON.stringify(filter), page],
  );
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<Showtime>();
  const action = useAction();

  const setF = (patch: Partial<typeof filter>) => { setFilter({ ...filter, ...patch }); setPage(0); };

  return (
    <>
      <div className="row space">
        <h1>Suất chiếu</h1>
        <button className="primary" onClick={() => setCreating(true)}>+ Tạo suất chiếu</button>
      </div>
      <div className="row wrap">
        <select value={filter.movieId} onChange={(e) => setF({ movieId: e.target.value })}>
          <option value="">Mọi phim (đang chiếu)</option>
          {movies.data?.items.map((m) => <option key={m.id} value={m.id}>{m.title}</option>)}
        </select>
        <select value={filter.cinemaId} onChange={(e) => setF({ cinemaId: e.target.value })}>
          <option value="">Mọi rạp</option>
          {cinemas.data?.items.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input type="date" value={filter.date} onChange={(e) => setF({ date: e.target.value })} title="Ngày theo giờ Việt Nam (để trống = mọi ngày)" />
        {STATUSES.map((s) => (
          <label key={s} className="check inline">
            <input type="checkbox" checked={filter.status.includes(s)}
              onChange={() => setF({ status: filter.status.includes(s) ? filter.status.filter((x) => x !== s) : [...filter.status, s] })} />
            {label(s)}
          </label>
        ))}
      </div>
      <ErrorBox error={list.error ?? action.error} />
      {list.loading ? <Loading /> : (
        <table className="table">
          <thead><tr><th>Bắt đầu</th><th>Kết thúc</th><th>Phim</th><th>Rạp · phòng</th><th>Giá cơ bản</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            {list.data?.items.map((s) => (
              <tr key={s.id}>
                <td>{fmtDateTime(s.startTime)}</td>
                <td>{fmtDateTime(s.endTime)}</td>
                <td>{s.movieTitle}</td>
                <td>{s.cinemaName} · {s.screenName}</td>
                <td>{fmtMoney(s.basePrice)}</td>
                <td><StatusBadge value={s.status} /></td>
                <td className="row">
                  <Link to={`/showtimes/${s.id}`}>Ghế</Link>
                  {(s.status === 'SCHEDULED' || s.status === 'SELLING') && <>
                    <button className="small" onClick={() => setEditing(s)}>Sửa</button>
                    <button className="small danger" disabled={action.busy} onClick={() => action.run(async () => {
                      if (!confirm('Huỷ suất chiếu này? Mọi ghế sẽ bị khoá.')) return;
                      await showtimesApi.cancel(s.id);
                      await list.reload();
                    })}>Huỷ suất</button>
                  </>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {list.data?.items.length === 0 && <p className="muted">Không có suất chiếu.</p>}
      {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />}

      {creating && <CreateShowtime movies={movies.data?.items ?? []} cinemas={cinemas.data?.items ?? []}
        onClose={() => setCreating(false)} onDone={() => { setCreating(false); list.reload(); }} />}
      {editing && <EditShowtime showtime={editing} onClose={() => setEditing(undefined)} onDone={() => { setEditing(undefined); list.reload(); }} />}
    </>
  );
}

function CreateShowtime({ movies, cinemas, onClose, onDone }: {
  movies: { id: string; title: string; durationMinutes: number }[]; cinemas: { id: string; name: string }[]; onClose: () => void; onDone: () => void;
}) {
  const [form, setForm] = useState({ movieId: '', cinemaId: '', screenId: '', start: '', basePrice: 90000 });
  const [screens, setScreens] = useState<Screen[]>([]);
  const save = useAction();

  useEffect(() => {
    setScreens([]);
    if (form.cinemaId) cinemasApi.screens(form.cinemaId).then((s) => setScreens(s.filter((x) => x.status === 'ACTIVE'))).catch(save.setError);
  }, [form.cinemaId]); // eslint-disable-line react-hooks/exhaustive-deps

  const movie = movies.find((m) => m.id === form.movieId);

  return (
    <Modal title="Tạo suất chiếu" onClose={onClose}>
      <form className="form" onSubmit={(e) => {
        e.preventDefault();
        save.run(async () => {
          await showtimesApi.create({ movieId: form.movieId, cinemaId: form.cinemaId, screenId: form.screenId, startTime: vnLocalToIso(form.start), basePrice: form.basePrice });
          onDone();
        });
      }}>
        <label>Phim (đang chiếu) *
          <select required value={form.movieId} onChange={(e) => setForm({ ...form, movieId: e.target.value })}>
            <option value="">— chọn —</option>
            {movies.map((m) => <option key={m.id} value={m.id}>{m.title} ({m.durationMinutes}′)</option>)}
          </select>
        </label>
        <label>Rạp *
          <select required value={form.cinemaId} onChange={(e) => setForm({ ...form, cinemaId: e.target.value, screenId: '' })}>
            <option value="">— chọn —</option>
            {cinemas.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label>Phòng *
          <select required value={form.screenId} onChange={(e) => setForm({ ...form, screenId: e.target.value })}>
            <option value="">— chọn —</option>
            {screens.map((s) => <option key={s.id} value={s.id}>{s.name} · {s.screenTypeName} · {s.totalSeats} ghế</option>)}
          </select>
        </label>
        <label>Giờ bắt đầu (giờ Việt Nam) *<input type="datetime-local" required value={form.start} onChange={(e) => setForm({ ...form, start: e.target.value })} /></label>
        <FieldError error={save.error} field="startTime" />
        {movie && form.start && <p className="muted small">Kết thúc dự kiến: {fmtDateTime(new Date(new Date(vnLocalToIso(form.start)).getTime() + movie.durationMinutes * 60000).toISOString())}</p>}
        <label>Giá cơ bản (VND) *<input type="number" min={0} step={1000} required value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })} /></label>
        <FieldError error={save.error} field="basePrice" />
        <p className="muted small">Giá từng ghế = giá cơ bản × hệ số loại ghế. Các suất cùng phòng phải cách nhau ≥ 15 phút.</p>
        <ErrorBox error={save.error} />
        <button className="primary" disabled={save.busy}>Tạo</button>
      </form>
    </Modal>
  );
}

function EditShowtime({ showtime, onClose, onDone }: { showtime: Showtime; onClose: () => void; onDone: () => void }) {
  const [start, setStart] = useState(isoToVnLocal(showtime.startTime));
  const [basePrice, setBasePrice] = useState(showtime.basePrice);
  const save = useAction();
  return (
    <Modal title={`Sửa suất – ${showtime.movieTitle}`} onClose={onClose}>
      <form className="form" onSubmit={(e) => {
        e.preventDefault();
        save.run(async () => { await showtimesApi.update(showtime.id, { startTime: vnLocalToIso(start), basePrice }); onDone(); });
      }}>
        <label>Giờ bắt đầu (giờ Việt Nam)<input type="datetime-local" required value={start} onChange={(e) => setStart(e.target.value)} /></label>
        <label>Giá cơ bản<input type="number" min={0} step={1000} value={basePrice} onChange={(e) => setBasePrice(Number(e.target.value))} /></label>
        <p className="muted small">Không sửa được khi đã có ghế đang giữ hoặc đã bán.</p>
        <ErrorBox error={save.error} />
        <button className="primary" disabled={save.busy}>Lưu</button>
      </form>
    </Modal>
  );
}
