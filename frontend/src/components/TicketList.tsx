import type { Booking } from '../api/types';
import { fmtMoney } from '../utils/format';

export function TicketList({ booking }: { booking: Booking }) {
  return (
    <table className="table">
      <thead><tr><th>Ghế</th><th>Giá</th><th>Mã vé</th></tr></thead>
      <tbody>
        {booking.seats.map((s) => (
          <tr key={s.id}>
            <td>{s.seatLabel}</td>
            <td>{fmtMoney(s.unitPrice, booking.currency)}</td>
            <td><code>{s.ticketCode ?? '—'}</code></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
