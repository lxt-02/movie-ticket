import { useParams } from 'react-router-dom';
import { cinemasApi } from '../api/endpoints';
import { ErrorBox, Loading, StatusBadge, useAsync } from '../components/ui';
import { ShowtimeBrowser } from '../components/ShowtimeBrowser';

/** GET /api/v1/cinemas/{id}, /screens + showtimes at the cinema (public) */
export default function CinemaDetailPage() {
  const { id = '' } = useParams();
  const cinema = useAsync(() => cinemasApi.get(id), [id]);
  const screens = useAsync(() => cinemasApi.screens(id), [id]);

  if (cinema.loading) return <Loading />;
  if (!cinema.data) return <ErrorBox error={cinema.error} />;
  const c = cinema.data;

  return (
    <>
      <div className="card">
        <h1>{c.name}</h1>
        <div>{c.address}{c.district ? `, ${c.district}` : ''}, {c.city}</div>
        {c.phone && <div>☎ {c.phone}</div>}
        <div className="row small">
          {screens.data?.map((s) => (
            <span key={s.id} className="chip">{s.name} · {s.screenTypeName} · {s.totalSeats} ghế <StatusBadge value={s.status} /></span>
          ))}
        </div>
      </div>
      <h2>Lịch chiếu tại rạp</h2>
      <ShowtimeBrowser cinemaId={c.id} groupBy="movie" />
    </>
  );
}
