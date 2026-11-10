import { http } from './client';
import type {
  Booking, Cinema, CinemaRequest, CinemaStatus, Genre, LayoutRow, Movie, MovieRequest, MovieStatus, Page,
  Payment, Person, Role, Screen, ScreenLayout, ScreenStatus, ScreenType, SearchResult, Suggestion, SeatType,
  Showtime, ShowtimeSeat, ShowtimeStatus, User, UserStatus,
} from './types';

/* ------------------------------------------------------------------ users §4.1 */
export const usersApi = {
  /** must be called ONCE right after login: creates the profile */
  me: () => http.get<User>('/api/v1/users/me'),
  updateMe: (body: { fullName: string; avatarUrl?: string }) => http.put<User>('/api/v1/users/me', body),
  // ADMIN
  list: (q: { q?: string; role?: Role; status?: UserStatus; page?: number; size?: number }) =>
    http.get<Page<User>>('/api/v1/users', q),
  get: (id: string) => http.get<User>(`/api/v1/users/${id}`),
  setStatus: (id: string, status: UserStatus) => http.patch<User>(`/api/v1/users/${id}/status`, { status }),
};

/* ------------------------------------------------------------------ movies §4.2 */
export const moviesApi = {
  list: (q: { status?: MovieStatus; q?: string; page?: number; size?: number }) =>
    http.get<Page<Movie>>('/api/v1/movies', q),
  get: (id: string) => http.get<Movie>(`/api/v1/movies/${id}`),
  genres: () => http.get<Genre[]>('/api/v1/movies/genres'),
  persons: (q: { q?: string; page?: number; size?: number }) => http.get<Page<Person>>('/api/v1/movies/persons', q),
  // ADMIN
  create: (body: MovieRequest) => http.post<Movie>('/api/v1/movies', body),
  update: (id: string, body: MovieRequest) => http.put<Movie>(`/api/v1/movies/${id}`, body),
  setStatus: (id: string, status: MovieStatus) => http.patch<Movie>(`/api/v1/movies/${id}/status`, { status }),
  archive: (id: string) => http.delete<unknown>(`/api/v1/movies/${id}`),
  createGenre: (name: string) => http.post<Genre>('/api/v1/movies/genres', { name }),
  createPerson: (body: { name: string; avatarUrl?: string; biography?: string }) =>
    http.post<Person>('/api/v1/movies/persons', body),
  searchSync: () => http.post<unknown>('/api/v1/movies/search-sync'),
};

/* ------------------------------------------------------------------ search §4.3 */
export interface SearchQuery {
  q?: string;
  person?: string;
  genres?: string[];
  status?: string[];
  ageRating?: string[];
  language?: string[];
  country?: string[];
  releaseFrom?: string;
  releaseTo?: string;
  durationMin?: number;
  durationMax?: number;
  sort?: 'RELEVANCE' | 'NEWEST' | 'OLDEST' | 'TITLE' | 'DURATION_ASC' | 'DURATION_DESC';
  page?: number;
  size?: number;
}
export const searchApi = {
  movies: (q: SearchQuery) => http.get<SearchResult>('/api/v1/search/movies', { ...q }),
  suggest: (q: string, size = 5) => http.get<Suggestion[]>('/api/v1/search/movies/suggest', { q, size }),
  // ADMIN
  recreateIndex: () => http.post<unknown>('/api/v1/search/admin/movies/recreate-index'),
};

/* ------------------------------------------------------------------ cinemas / screens §4.4 */
export const cinemasApi = {
  list: (q: { city?: string; status?: CinemaStatus; q?: string; page?: number; size?: number }) =>
    http.get<Page<Cinema>>('/api/v1/cinemas', q),
  get: (id: string) => http.get<Cinema>(`/api/v1/cinemas/${id}`),
  screens: (id: string) => http.get<Screen[]>(`/api/v1/cinemas/${id}/screens`),
  // ADMIN
  create: (body: CinemaRequest) => http.post<Cinema>('/api/v1/cinemas', body),
  update: (id: string, body: CinemaRequest) => http.put<Cinema>(`/api/v1/cinemas/${id}`, body),
  setStatus: (id: string, status: CinemaStatus) => http.patch<Cinema>(`/api/v1/cinemas/${id}/status`, { status }),
  close: (id: string) => http.delete<unknown>(`/api/v1/cinemas/${id}`),
};

export const screensApi = {
  get: (id: string) => http.get<Screen>(`/api/v1/screens/${id}`),
  layout: (id: string) => http.get<ScreenLayout>(`/api/v1/screens/${id}/layout`),
  screenTypes: () => http.get<ScreenType[]>('/api/v1/screen-types'),
  seatTypes: () => http.get<SeatType[]>('/api/v1/seat-types'),
  // ADMIN
  create: (cinemaId: string, body: { name: string; screenTypeId: string; rows: LayoutRow[] }) =>
    http.post<Screen>(`/api/v1/cinemas/${cinemaId}/screens`, body),
  update: (id: string, body: { name?: string; screenTypeId?: string }) => http.put<Screen>(`/api/v1/screens/${id}`, body),
  setStatus: (id: string, status: ScreenStatus) => http.patch<Screen>(`/api/v1/screens/${id}/status`, { status }),
  replaceLayout: (id: string, rows: LayoutRow[]) => http.put<ScreenLayout>(`/api/v1/screens/${id}/layout`, { rows }),
  setSeatStatus: (id: string, seatId: string, status: 'ACTIVE' | 'BROKEN') =>
    http.patch<unknown>(`/api/v1/screens/${id}/seats/${seatId}/status`, { status }),
  deactivate: (id: string) => http.delete<unknown>(`/api/v1/screens/${id}`),
};

/* ------------------------------------------------------------------ showtimes §4.5 */
export const showtimesApi = {
  list: (q: {
    movieId?: string; cinemaId?: string; screenId?: string; date?: string; from?: string; to?: string;
    status?: ShowtimeStatus[]; page?: number; size?: number;
  }) => http.get<Page<Showtime>>('/api/v1/showtimes', q),
  get: (id: string) => http.get<Showtime>(`/api/v1/showtimes/${id}`),
  seats: (id: string) => http.get<ShowtimeSeat[]>(`/api/v1/showtimes/${id}/seats`),
  // ADMIN
  create: (body: { movieId: string; cinemaId: string; screenId: string; startTime: string; basePrice: number }) =>
    http.post<Showtime>('/api/v1/showtimes', body),
  update: (id: string, body: { startTime?: string; basePrice?: number }) => http.put<Showtime>(`/api/v1/showtimes/${id}`, body),
  cancel: (id: string) => http.delete<unknown>(`/api/v1/showtimes/${id}`),
};

/* ------------------------------------------------------------------ bookings §4.6 */
export const bookingsApi = {
  create: (body: { showtimeId: string; seats: { showtimeSeatId: string }[] }, idempotencyKey: string) =>
    http.post<Booking>('/api/v1/bookings', body, { 'Idempotency-Key': idempotencyKey }),
  mine: () => http.get<Booking[]>('/api/v1/bookings'),
  get: (id: string) => http.get<Booking>(`/api/v1/bookings/${id}`),
  byCode: (code: string) => http.get<Booking>(`/api/v1/bookings/code/${encodeURIComponent(code)}`),
  cancel: (id: string, reason?: string) => http.post<Booking>(`/api/v1/bookings/${id}/cancel`, reason ? { reason } : undefined),
  retryPayment: (id: string) => http.post<unknown>(`/api/v1/bookings/${id}/payment-retry`),
};

/* ------------------------------------------------------------------ payments §4.7 */
export const paymentsApi = {
  byBooking: (bookingId: string) => http.get<Payment>(`/api/v1/payments/by-booking/${bookingId}`),
  syncByBooking: (bookingId: string) => http.post<Payment>(`/api/v1/payments/by-booking/${bookingId}/sync`),
  get: (id: string) => http.get<Payment>(`/api/v1/payments/${id}`),
  sync: (id: string) => http.post<Payment>(`/api/v1/payments/${id}/sync`),
};

/* ------------------------------------------------------------------ ops (ADMIN / STAFF) */
export type OpsView = Record<string, unknown>;
export const opsApi = {
  dltStatus: () => http.get<OpsView>('/api/v1/bookings/admin/payment-events-dlt'),
  dltReplay: () => http.post<OpsView>('/api/v1/bookings/admin/payment-events-dlt/replay'),
  dltStop: () => http.post<OpsView>('/api/v1/bookings/admin/payment-events-dlt/stop'),
  bookingOutbox: () => http.get<OpsView>('/api/v1/bookings/admin/outbox'),
  bookingOutboxRequeue: () => http.post<OpsView>('/api/v1/bookings/admin/outbox/requeue-failed'),
  paymentOutbox: () => http.get<OpsView>('/api/v1/payments/admin/outbox'),
  paymentOutboxRequeue: () => http.post<OpsView>('/api/v1/payments/admin/outbox/requeue-failed'),
  chaos: (fail: boolean) => http.post<OpsView>('/api/v1/bookings/admin/chaos/payment-events', undefined, undefined, { fail }),
};
