import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { screensApi } from '../../api/endpoints';
import type { LayoutRow, LayoutSeat } from '../../api/types';
import { ErrorBox, Loading, StatusBadge, useAction, useAsync } from '../../components/ui';
import { RowsEditor } from '../../components/RowsEditor';

/**
 * ADMIN: GET /screens/{id}/layout, PATCH seat status (ACTIVE ↔ BROKEN), PUT /screens/{id} (name / type),
 * PUT /screens/{id}/layout (only when the screen is NOT ACTIVE → switch to MAINTENANCE first)
 */
export default function AdminScreenLayout() {
  const { id = '' } = useParams();
  const layout = useAsync(() => screensApi.layout(id), [id]);
  const seatTypes = useAsync(() => screensApi.seatTypes(), []);
  const screenTypes = useAsync(() => screensApi.screenTypes(), []);
  const [rows, setRows] = useState<LayoutRow[]>();
  const [meta, setMeta] = useState<{ name: string; screenTypeId: string }>();
  const action = useAction();

  const grid = useMemo(() => {
    const map = new Map<string, LayoutSeat[]>();
    for (const s of layout.data?.seats ?? []) map.set(s.rowLabel, [...(map.get(s.rowLabel) ?? []), s]);
    return [...map.entries()];
  }, [layout.data]);
  const maxCol = Math.max(0, ...(layout.data?.seats ?? []).map((s) => s.seatNumber));

  if (layout.loading && !layout.data) return <Loading />;
  if (!layout.data) return <ErrorBox error={layout.error} />;
  const screen = layout.data.screen;

  const startEditRows = () =>
    setRows(grid.map(([rowLabel, seats]) => ({ rowLabel, seatCount: seats.length, seatTypeId: seats[0].seatTypeId })));

  return (
    <>
      <Link to={`/admin/cinemas/${screen.cinemaId}`}>‹ Về danh sách phòng</Link>
      <div className="card">
        <h1>{screen.name} <StatusBadge value={screen.status} /></h1>
        <div>{screen.screenTypeName} · {screen.totalSeats} ghế</div>
        {meta ? (
          <form className="row" onSubmit={(e) => { e.preventDefault(); action.run(async () => { await screensApi.update(id, meta); setMeta(undefined); await layout.reload(); }); }}>
            <input value={meta.name} onChange={(e) => setMeta({ ...meta, name: e.target.value })} />
            <select value={meta.screenTypeId} onChange={(e) => setMeta({ ...meta, screenTypeId: e.target.value })}>
              {screenTypes.data?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
            <button className="primary" disabled={action.busy}>Lưu</button>
            <button type="button" className="ghost" onClick={() => setMeta(undefined)}>Huỷ</button>
          </form>
        ) : <button className="small" onClick={() => setMeta({ name: screen.name, screenTypeId: screen.screenTypeId })}>Sửa tên / loại phòng</button>}
      </div>

      <h3>Sơ đồ ghế <span className="muted small">(bấm vào ghế để đánh dấu hỏng / sửa xong)</span></h3>
      <div className="screen-bar">MÀN HÌNH</div>
      <div className="seat-map" style={{ gridTemplateColumns: `2rem repeat(${maxCol}, 2.2rem)` }}>
        {grid.map(([row, seats]) => {
          const byCol = new Map(seats.map((s) => [s.seatNumber, s]));
          return [
            <div key={row} className="row-label">{row}</div>,
            ...Array.from({ length: maxCol }, (_, i) => {
              const s = byCol.get(i + 1);
              if (!s) return <div key={row + i} />;
              return (
                <button key={s.id} className={`seat ${s.status === 'ACTIVE' ? 'available' : 'blocked'} type-${s.seatTypeName.toLowerCase()}`}
                  title={`${s.label} · ${s.seatTypeName} ×${s.priceMultiplier} · ${s.status}`}
                  disabled={action.busy || s.status === 'INACTIVE'}
                  onClick={() => action.run(async () => { await screensApi.setSeatStatus(id, s.id, s.status === 'ACTIVE' ? 'BROKEN' : 'ACTIVE'); await layout.reload(); })}>
                  {s.seatNumber}
                </button>
              );
            }),
          ];
        })}
      </div>
      <p className="muted small">Thay đổi sơ đồ chỉ ảnh hưởng suất chiếu tạo sau đó.</p>

      <div className="card">
        <h3>Thay sơ đồ ghế</h3>
        {screen.status === 'ACTIVE' ? (
          <p className="warn-text">Phòng đang hoạt động. Chuyển phòng sang "Bảo trì" trước khi đổi sơ đồ.
            {' '}<button className="small" disabled={action.busy} onClick={() => action.run(async () => { await screensApi.setStatus(id, 'MAINTENANCE'); await layout.reload(); })}>Chuyển sang bảo trì</button></p>
        ) : rows ? (
          <>
            <RowsEditor rows={rows} onChange={setRows} seatTypes={seatTypes.data ?? []} />
            <div className="row">
              <button className="primary" disabled={action.busy} onClick={() => action.run(async () => {
                await screensApi.replaceLayout(id, rows);
                setRows(undefined);
                await layout.reload();
              })}>Lưu sơ đồ mới</button>
              <button className="ghost" onClick={() => setRows(undefined)}>Huỷ</button>
            </div>
          </>
        ) : (
          <div className="row">
            <button onClick={startEditRows}>Sửa sơ đồ</button>
            <button disabled={action.busy} onClick={() => action.run(async () => { await screensApi.setStatus(id, 'ACTIVE'); await layout.reload(); })}>Kích hoạt lại phòng</button>
          </div>
        )}
      </div>
      <ErrorBox error={action.error} />
    </>
  );
}
