import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { cinemasApi, screensApi } from '../../api/endpoints';
import type { LayoutRow, ScreenStatus } from '../../api/types';
import { ErrorBox, FieldError, Loading, Modal, StatusBadge, useAction, useAsync } from '../../components/ui';
import { RowsEditor } from '../../components/RowsEditor';
import { label } from '../../utils/format';

/** ADMIN: screens of a cinema. POST /cinemas/{id}/screens, PATCH /screens/{id}/status, DELETE /screens/{id} */
export default function AdminCinemaScreens() {
  const { id = '' } = useParams();
  const cinema = useAsync(() => cinemasApi.get(id), [id]);
  const screens = useAsync(() => cinemasApi.screens(id), [id]);
  const screenTypes = useAsync(() => screensApi.screenTypes(), []);
  const seatTypes = useAsync(() => screensApi.seatTypes(), []);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [screenTypeId, setScreenTypeId] = useState('');
  const [rows, setRows] = useState<LayoutRow[]>([]);
  const action = useAction();
  const save = useAction();

  const startCreate = () => {
    const st = seatTypes.data?.[0]?.id ?? '';
    setName('');
    setScreenTypeId(screenTypes.data?.[0]?.id ?? '');
    setRows(['A', 'B', 'C', 'D', 'E'].map((r) => ({ rowLabel: r, seatCount: 10, seatTypeId: st })));
    setCreating(true);
  };

  if (cinema.loading) return <Loading />;
  if (!cinema.data) return <ErrorBox error={cinema.error} />;

  return (
    <>
      <div className="row space">
        <h1>Phòng chiếu – {cinema.data.name} <StatusBadge value={cinema.data.status} /></h1>
        {cinema.data.status !== 'CLOSED' && <button className="primary" onClick={startCreate}>+ Thêm phòng</button>}
      </div>
      <ErrorBox error={screens.error ?? action.error} />
      <table className="table">
        <thead><tr><th>Tên</th><th>Loại</th><th>Số ghế</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>
          {screens.data?.map((s) => (
            <tr key={s.id}>
              <td>{s.name}</td>
              <td>{s.screenTypeName}</td>
              <td>{s.totalSeats}</td>
              <td><StatusBadge value={s.status} /></td>
              <td className="row">
                <Link to={`/admin/screens/${s.id}`}>Sơ đồ ghế</Link>
                {(['ACTIVE', 'MAINTENANCE'] as ScreenStatus[]).filter((x) => x !== s.status).map((x) => (
                  <button key={x} className="small" disabled={action.busy}
                    onClick={() => action.run(async () => { await screensApi.setStatus(s.id, x); await screens.reload(); })}>
                    → {label(x)}
                  </button>
                ))}
                {s.status !== 'INACTIVE' && (
                  <button className="small danger" disabled={action.busy} onClick={() => action.run(async () => {
                    if (!confirm(`Ngừng dùng phòng "${s.name}"?`)) return;
                    await screensApi.deactivate(s.id);
                    await screens.reload();
                  })}>Ngừng dùng</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {creating && (
        <Modal title="Thêm phòng chiếu" onClose={() => setCreating(false)}>
          <form className="form" onSubmit={(e) => {
            e.preventDefault();
            save.run(async () => {
              await screensApi.create(id, { name, screenTypeId, rows });
              setCreating(false);
              await screens.reload();
            });
          }}>
            <label>Tên phòng *<input required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></label>
            <FieldError error={save.error} field="name" />
            <label>Loại phòng *
              <select required value={screenTypeId} onChange={(e) => setScreenTypeId(e.target.value)}>
                {screenTypes.data?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </label>
            <RowsEditor rows={rows} onChange={setRows} seatTypes={seatTypes.data ?? []} />
            <FieldError error={save.error} field="rows" />
            <ErrorBox error={save.error} />
            <button className="primary" disabled={save.busy}>Tạo phòng</button>
          </form>
        </Modal>
      )}
    </>
  );
}
