import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { RequireRole } from './auth/RequireRole';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import MovieDetailPage from './pages/MovieDetailPage';
import CinemasPage from './pages/CinemasPage';
import CinemaDetailPage from './pages/CinemaDetailPage';
import SeatSelectionPage from './pages/SeatSelectionPage';
import CheckoutPage from './pages/CheckoutPage';
import PaymentReturnPage from './pages/PaymentReturnPage';
import MyBookingsPage from './pages/MyBookingsPage';
import BookingDetailPage from './pages/BookingDetailPage';
import ProfilePage from './pages/ProfilePage';
import NotFoundPage from './pages/NotFoundPage';
import OpsPage from './pages/staff/OpsPage';
import AdminHome from './pages/admin/AdminHome';
import AdminMovies from './pages/admin/AdminMovies';
import AdminMovieForm from './pages/admin/AdminMovieForm';
import AdminCatalog from './pages/admin/AdminCatalog';
import AdminCinemas from './pages/admin/AdminCinemas';
import AdminCinemaScreens from './pages/admin/AdminCinemaScreens';
import AdminScreenLayout from './pages/admin/AdminScreenLayout';
import AdminShowtimes from './pages/admin/AdminShowtimes';
import AdminUsers from './pages/admin/AdminUsers';

/*
 * Who can open what (backend checks again on every API call):
 *  - Guest:     movies, search, cinemas, showtimes, seat map
 *  - Logged in: + booking, payment, my bookings, profile    (CUSTOMER / STAFF / ADMIN)
 *  - STAFF:     + /ops (booking & payment outbox, payment-events DLT)
 *  - ADMIN:     + /ops and /admin/** (movies, cinemas, screens, layout, showtimes, users)
 */
export default function App() {
  const auth = (el: JSX.Element) => <RequireRole>{el}</RequireRole>;
  const ops = (el: JSX.Element) => <RequireRole roles={['ADMIN', 'STAFF']}>{el}</RequireRole>;
  const admin = (el: JSX.Element) => <RequireRole roles={['ADMIN']}>{el}</RequireRole>;

  return (
    <Routes>
      <Route element={<Layout />}>
        {/* public */}
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/movies/:id" element={<MovieDetailPage />} />
        <Route path="/cinemas" element={<CinemasPage />} />
        <Route path="/cinemas/:id" element={<CinemaDetailPage />} />
        <Route path="/showtimes/:id" element={<SeatSelectionPage />} />

        {/* logged in */}
        <Route path="/checkout/:bookingId" element={auth(<CheckoutPage />)} />
        <Route path="/payment/success" element={auth(<PaymentReturnPage kind="success" />)} />
        <Route path="/payment/cancel" element={auth(<PaymentReturnPage kind="cancel" />)} />
        <Route path="/my-bookings" element={auth(<MyBookingsPage />)} />
        <Route path="/my-bookings/:id" element={auth(<BookingDetailPage />)} />
        <Route path="/profile" element={auth(<ProfilePage />)} />

        {/* STAFF + ADMIN */}
        <Route path="/ops" element={ops(<OpsPage />)} />

        {/* ADMIN */}
        <Route path="/admin" element={admin(<AdminHome />)} />
        <Route path="/admin/movies" element={admin(<AdminMovies />)} />
        <Route path="/admin/movies/new" element={admin(<AdminMovieForm />)} />
        <Route path="/admin/movies/:id" element={admin(<AdminMovieForm />)} />
        <Route path="/admin/catalog" element={admin(<AdminCatalog />)} />
        <Route path="/admin/cinemas" element={admin(<AdminCinemas />)} />
        <Route path="/admin/cinemas/:id" element={admin(<AdminCinemaScreens />)} />
        <Route path="/admin/screens/:id" element={admin(<AdminScreenLayout />)} />
        <Route path="/admin/showtimes" element={admin(<AdminShowtimes />)} />
        <Route path="/admin/users" element={admin(<AdminUsers />)} />

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
