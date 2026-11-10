import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { bookingsApi } from '../api/endpoints';
import { ErrorBox, Loading, StatusBadge, useAction, useAsync } from '../components/ui';
import { fmtDateTime, fmtMoney } from '../utils/format';

/** GET /api/v1/bookings (own bookings, newest first) + GET /bookings/code/{code} */
export default function MyBookingsPage() {
  const { data, error, loading } = useAsync(() => bookingsApi.mine(), []);
  const [code, setCode] = useState('');
  const navigate = useNavigate();
  const find = useAction();

  return (
    <>
      <h1>Vé của tôi</h1>
      <form className="row" onSubmit={(e) => {
        e.preventDefault();
        find.run(async () => navigate(`/my-bookings/${(await bookingsApi.byCode(code.trim())).id}`));
      }}>
        <input placeholder="Tra cứu theo mã đặt vé…" value={code} onChange={(e) => setCode(e.target.value)} />
        <button disabled={!code.trim() || find.busy}>Tra cứu</button>
      </form>
      <ErrorBox error={find.error} />
      <ErrorBox error={error} />
      {loading ? <Loading /> : data?.length === 0 ? <p className="muted">Bạn chưa có đơn đặt vé nào.</p> : (
        <table className="table">
          <thead><tr><th>Mã</th><th>Phim</th><th>Rạp / phòng</th><th>Suất</th><th>Ghế</th><th>Tổng</th><th>Trạng thái</th><th /></tr></thead>
          <tbody>
            {data?.map((b) => (
              <tr key={b.id}>
                <td><code>{b.bookingCode}</code></td>
                <td>{b.movieTitle}</td>
                <td>{b.cinemaName} · {b.screenName}</td>
                <td>{fmtDateTime(b.startTime)}</td>
                <td>{b.seats.map((s) => s.seatLabel).join(', ')}</td>
                <td>{fmtMoney(b.totalAmount, b.currency)}</td>
                <td><StatusBadge value={b.status} /></td>
                <td>
                  {b.status === 'PENDING' ? <Link to={`/checkout/${b.id}`}>Thanh toán</Link> : <Link to={`/my-bookings/${b.id}`}>Chi tiết</Link>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
