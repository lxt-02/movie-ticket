import { Link, useParams } from 'react-router-dom';
import { bookingsApi, paymentsApi } from '../api/endpoints';
import { ErrorBox, Loading, StatusBadge, useAction, useAsync } from '../components/ui';
import { TicketList } from '../components/TicketList';
import { fmtDateTime, fmtMoney } from '../utils/format';

/** GET /bookings/{id} (other users' bookings → 404) + latest payment */
export default function BookingDetailPage() {
  const { id = '' } = useParams();
  const booking = useAsync(() => bookingsApi.get(id), [id]);
  const payment = useAsync(() => paymentsApi.byBooking(id).catch(() => undefined), [id]);
  const action = useAction();

  if (booking.loading && !booking.data) return <Loading />;
  const b = booking.data;
  if (!b) return <ErrorBox error={booking.error} />;
  const p = payment.data;

  return (
    <div className="card">
      <h2>Đơn {b.bookingCode} <StatusBadge value={b.status} /></h2>
      <dl className="info">
        <dt>Phim</dt><dd>{b.movieTitle}</dd>
        <dt>Rạp / phòng</dt><dd>{b.cinemaName} · {b.screenName}</dd>
        <dt>Suất chiếu</dt><dd>{fmtDateTime(b.startTime)}</dd>
        <dt>Tạm tính</dt><dd>{fmtMoney(b.subtotal, b.currency)}</dd>
        <dt>Giảm giá</dt><dd>{fmtMoney(b.discountAmount, b.currency)}</dd>
        <dt>Tổng tiền</dt><dd><strong>{fmtMoney(b.totalAmount, b.currency)}</strong></dd>
        <dt>Tạo lúc</dt><dd>{fmtDateTime(b.createdAt)}</dd>
        {b.confirmedAt && <><dt>Xác nhận lúc</dt><dd>{fmtDateTime(b.confirmedAt)}</dd></>}
        {b.cancelledAt && <><dt>Huỷ lúc</dt><dd>{fmtDateTime(b.cancelledAt)}</dd></>}
        {p && <><dt>Thanh toán</dt><dd>{p.paymentCode} · <StatusBadge value={p.status} /> {p.paidAt && `· ${fmtDateTime(p.paidAt)}`}</dd></>}
      </dl>
      <TicketList booking={b} />
      {b.status === 'PENDING' && (
        <div className="row actions">
          <Link to={`/checkout/${b.id}`} className="button primary">Tiếp tục thanh toán</Link>
          <button className="danger" disabled={action.busy} onClick={() => action.run(async () => {
            if (!confirm('Huỷ đơn này?')) return;
            booking.setData(await bookingsApi.cancel(b.id, 'Khách huỷ trên web'));
          })}>Huỷ đơn</button>
        </div>
      )}
      {(b.status === 'CANCELLED' || b.status === 'EXPIRED') && p?.status === 'SUCCESS' && (
        <p className="warn-text">Đơn đã thanh toán nhưng không thành công – vui lòng liên hệ quầy vé với mã {b.bookingCode}.</p>
      )}
      <ErrorBox error={action.error} />
    </div>
  );
}
