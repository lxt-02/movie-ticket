import type { LayoutRow, SeatType } from '../api/types';

/** Seat layout declared by rows: [{ rowLabel, seatCount (1–50), seatTypeId }] → seats A1..An (max 600 seats) */
export function RowsEditor({ rows, onChange, seatTypes }: { rows: LayoutRow[]; onChange: (r: LayoutRow[]) => void; seatTypes: SeatType[] }) {
  const total = rows.reduce((s, r) => s + (r.seatCount || 0), 0);
  const nextLabel = () => String.fromCharCode(65 + rows.length);
  const update = (i: number, patch: Partial<LayoutRow>) => onChange(rows.map((r, k) => (k === i ? { ...r, ...patch } : r)));

  return (
    <div>
      <table className="table compact">
        <thead><tr><th>Hàng</th><th>Số ghế</th><th>Loại ghế</th><th /></tr></thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              <td><input className="xs" value={r.rowLabel} maxLength={3} onChange={(e) => update(i, { rowLabel: e.target.value.toUpperCase() })} /></td>
              <td><input className="xs" type="number" min={1} max={50} value={r.seatCount} onChange={(e) => update(i, { seatCount: Number(e.target.value) })} /></td>
              <td>
                <select value={r.seatTypeId} onChange={(e) => update(i, { seatTypeId: e.target.value })}>
                  {seatTypes.map((t) => <option key={t.id} value={t.id}>{t.name} (×{t.priceMultiplier})</option>)}
                </select>
              </td>
              <td><button type="button" className="ghost" onClick={() => onChange(rows.filter((_, k) => k !== i))}>✕</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        <button type="button" onClick={() => onChange([...rows, { rowLabel: nextLabel(), seatCount: 10, seatTypeId: seatTypes[0]?.id ?? '' }])}>+ Thêm hàng</button>
        <span className={total > 600 ? 'warn-text' : 'muted'}>Tổng {total} ghế (tối đa 600)</span>
      </div>
    </div>
  );
}
