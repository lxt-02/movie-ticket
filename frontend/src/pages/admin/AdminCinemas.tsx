import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cinemasApi } from '../../api/endpoints';
import type { Cinema, CinemaRequest, CinemaStatus } from '../../api/types';
import { ErrorBox, FieldError, Loading, Modal, Pagination, StatusBadge, useAction, useAsync } from '../../components/ui';
import { label } from '../../utils/format';

const EMPTY: CinemaRequest = { name: '', brand: '', address: '', city: '', district: '', phone: '' };

/** ADMIN: cinemas CRUD (POST/PUT /cinemas, PATCH /status, DELETE = CLOSED) */
export default function AdminCinemas() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<CinemaStatus | ''>('');
  const [page, setPage] = useState(0);
  const list = useAsync(() => cinemasApi.list({ q, status: status || undefined, page, size: 20 }), [q, status, page]);
  const [editing, setEditing] = useState<{ id?: string; form: CinemaRequest }>();
  const action = useAction();
  const save = useAction();

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing) return;
    save.run(async () => {
      if (editing.id) await cinemasApi.update(editing.id, editing.form);
      else await cinemasApi.create({ ...editing.form, status: 'ACTIVE' });
      setEditing(undefined);
      await list.reload();
    });
  };
  const open = (c?: Cinema) =>
    setEditing({ id: c?.id, form: c ? { name: c.name, brand: c.brand ?? '', address: c.address, city: c.city, district: c.district ?? '', phone: c.phone ?? '', status: c.status } : EMPTY });
  const f = editing?.form;
  const setF = (k: keyof CinemaRequest, v: string) => setEditing((x) => x && { ...x, form: { ...x.form, [k]: v } });

  return (
    <>
      <div className="row space">
        <h1>Rạp chiếu</h1>
        <button className="primary" onClick={() => open()}>+ Thêm rạp</button>
      </div>
      <div className="row">
        <input placeholder="Tìm…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <select value={status} onChange={(e) => { setStatus(e.target.value as CinemaStatus | ''); setPage(0); }}>
          <option value="">Mọi trạng thái</option>
          {(['ACTIVE', 'INACTIVE', 'CLOSED'] as const).map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      <ErrorBox error={list.error ?? action.error} />
      {list.loading ? <Loading /> : (
        <table className="table">
          <thead><tr><th>Tên</th><th>Địa chỉ</th><th>Thành phố</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            {list.data?.items.map((c) => (
              <tr key={c.id}>
                <td><strong>{c.name}</strong><div className="muted small">{c.brand}</div></td>
                <td>{c.address}{c.district ? `, ${c.district}` : ''}</td>
                <td>{c.city}</td>
                <td><StatusBadge value={c.status} /></td>
                <td className="row">
                  <Link to={`/admin/cinemas/${c.id}`}>Phòng chiếu</Link>
                  {c.status !== 'CLOSED' && <>
                    <button className="small" onClick={() => open(c)}>Sửa</button>
                    <button className="small" disabled={action.busy} onClick={() => action.run(async () => {
                      await cinemasApi.setStatus(c.id, c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
                      await list.reload();
                    })}>{c.status === 'ACTIVE' ? 'Tạm ngưng' : 'Kích hoạt'}</button>
                    <button className="small danger" disabled={action.busy} onClick={() => action.run(async () => {
                      if (!confirm(`Đóng cửa rạp "${c.name}"? Không thêm / sửa phòng được nữa.`)) return;
                      await cinemasApi.close(c.id);
                      await list.reload();
                    })}>Đóng cửa</button>
                  </>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />}

      {f && (
        <Modal title={editing?.id ? 'Sửa rạp' : 'Thêm rạp'} onClose={() => setEditing(undefined)}>
          <form className="form" onSubmit={submit}>
            <label>Tên *<input required value={f.name} onChange={(e) => setF('name', e.target.value)} /></label>
            <FieldError error={save.error} field="name" />
            <label>Thương hiệu<input value={f.brand ?? ''} onChange={(e) => setF('brand', e.target.value)} /></label>
            <label>Địa chỉ *<input required value={f.address} onChange={(e) => setF('address', e.target.value)} /></label>
            <FieldError error={save.error} field="address" />
            <div className="grid-2">
              <label>Quận / huyện<input value={f.district ?? ''} onChange={(e) => setF('district', e.target.value)} /></label>
              <label>Thành phố *<input required value={f.city} onChange={(e) => setF('city', e.target.value)} /></label>
            </div>
            <label>Điện thoại<input value={f.phone ?? ''} onChange={(e) => setF('phone', e.target.value)} /></label>
            <ErrorBox error={save.error} />
            <button className="primary" disabled={save.busy}>Lưu</button>
          </form>
        </Modal>
      )}
    </>
  );
}
