/**
 * Client gọi backend qua API Gateway.
 * - Gọi đường dẫn tương đối /api/v1/... -> Vite proxy chuyển sang gateway http://localhost:8080 (không dính CORS).
 * - Gateway tự đọc token Keycloak và gắn X-User-Id / X-User-Role cho service, FE KHÔNG tự gửi các header này
 *   (gateway không cho phép chúng trong CORS và còn xoá đi).
 * - Danh sách của movie/cinema/showtime/user trả về PageResponse { items, page, size, totalElements, totalPages }.
 */
const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1';
const KC_BASE = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
const KC_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'mss301';
const KC_CLIENT = import.meta.env.VITE_KEYCLOAK_CLIENT || 'mss301-frontend';
const KC_TOKEN_URL = `${KC_BASE}/realms/${KC_REALM}/protocol/openid-connect/token`;

// ---------------------------------------------------------------- token / session
export const getAuthToken = () => localStorage.getItem('token');
export const getUserId = () => localStorage.getItem('userId') || null;
export const getUserRole = () => localStorage.getItem('userRole') || null;
export const isAuthenticated = () => !!localStorage.getItem('token');

export function parseJwt(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function saveTokens(data) {
  localStorage.setItem('token', data.access_token);
  if (data.refresh_token) localStorage.setItem('refreshToken', data.refresh_token);
  localStorage.setItem('tokenExpiresAt', String(Date.now() + (data.expires_in || 300) * 1000));

  const payload = parseJwt(data.access_token) || {};
  if (payload.sub) localStorage.setItem('userId', payload.sub);
  if (payload.preferred_username) localStorage.setItem('username', payload.preferred_username);
  if (payload.email) localStorage.setItem('userEmail', payload.email);
  const roles = payload.realm_access?.roles || [];
  const role = roles.includes('ADMIN') ? 'ADMIN' : roles.includes('STAFF') ? 'STAFF' : 'CUSTOMER';
  localStorage.setItem('userRole', role);
}

export function logout() {
  ['token', 'refreshToken', 'tokenExpiresAt', 'username', 'userEmail', 'userRole', 'userId',
    'userAvatar', 'userFullName'].forEach((k) => localStorage.removeItem(k));
}

async function tokenRequest(params) {
  const res = await fetch(KC_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: KC_CLIENT, ...params }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error_description || 'Tài khoản hoặc mật khẩu không chính xác');
  }
  return data;
}

/** Access token của Keycloak sống ~5 phút: làm mới bằng refresh token trước khi hết hạn. */
let refreshing = null;
async function ensureFreshToken() {
  const token = getAuthToken();
  if (!token) return null;
  const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0);
  if (Date.now() < expiresAt - 30_000) return token;
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return token;
  if (!refreshing) {
    refreshing = tokenRequest({ grant_type: 'refresh_token', refresh_token: refreshToken })
      .then((data) => {
        saveTokens(data);
        return data.access_token;
      })
      .catch(() => {
        logout();
        return null;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

// ---------------------------------------------------------------- http helper
export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

async function request(path, { method = 'GET', body, query, idempotencyKey } = {}) {
  let url = `${API_BASE}${path}`;
  if (query) {
    const qs = new URLSearchParams();
    Object.entries(query).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return;
      if (Array.isArray(v)) v.forEach((item) => item !== '' && qs.append(k, item));
      else qs.append(k, v);
    });
    const s = qs.toString();
    if (s) url += `?${s}`;
  }

  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
  const token = await ensureFreshToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError('Không kết nối được API Gateway (http://localhost:8080). Kiểm tra backend đã chạy chưa.', 0);
  }

  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }

  if (!res.ok) {
    if (res.status === 401 && token) logout();
    const message =
      json?.message ||
      json?.error ||
      (res.status === 401
        ? 'Bạn cần đăng nhập để thực hiện thao tác này'
        : res.status === 403
        ? 'Bạn không có quyền thực hiện thao tác này'
        : res.status === 429
        ? 'Bạn thao tác quá nhanh, vui lòng thử lại sau ít giây'
        : `Lỗi ${res.status} khi gọi ${path}`);
    throw new ApiError(message, res.status, json);
  }
  return json && Object.prototype.hasOwnProperty.call(json, 'data') ? json.data : json;
}

const itemsOf = (page) => (Array.isArray(page) ? page : page?.items || []);
const newIdempotencyKey = () =>
  (crypto.randomUUID && crypto.randomUUID()) || `bk-${Date.now()}-${Math.random().toString(36).slice(2)}`;

// ---------------------------------------------------------------- normalizers (DTO backend -> shape UI dùng)
const VN_TZ = 'Asia/Ho_Chi_Minh';
export const fmtTime = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: VN_TZ }) : '';
export const fmtDate = (iso) =>
  iso ? new Date(iso).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: VN_TZ }) : '';
export const fmtDateTime = (iso) => (iso ? `${fmtTime(iso)} • ${fmtDate(iso)}` : '');
export const fmtMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')} đ`;

const FALLBACK_POSTER = 'https://placehold.co/600x900/1e293b/f8fafc?text=MTK+Cinema';

/** MovieResponse (movie-service) hoặc Hit (search-service) -> movie cho UI. */
export function normalizeMovie(m) {
  if (!m) return null;
  const genres = (m.genres || []).map((g) => (typeof g === 'string' ? g : g.name));
  const directors = (m.directors || []).map((p) => (typeof p === 'string' ? p : p.name));
  const actors = (m.cast || m.actors || []).map((p) => (typeof p === 'string' ? p : p.name));
  return {
    ...m,
    genres,
    directorList: directors,
    actorList: actors,
    director: directors.join(', '),
    actors: actors.join(', '),
    synopsis: m.description || '',
    posterUrl: m.posterUrl || FALLBACK_POSTER,
    bannerUrl: m.posterUrl || FALLBACK_POSTER,
    format: [m.language, m.country].filter(Boolean).join(' • '),
  };
}

export function normalizeShowtime(st) {
  return {
    ...st,
    startAt: st.startTime,
    endAt: st.endTime,
    startTime: fmtTime(st.startTime),
    endTime: fmtTime(st.endTime),
    date: fmtDate(st.startTime),
    dateKey: st.startTime
      ? new Date(st.startTime).toLocaleDateString('en-CA', { timeZone: VN_TZ })
      : '',
  };
}

export function normalizeSeat(s) {
  return {
    ...s,
    id: s.showtimeSeatId,
    seatCode: s.seatLabel,
    rowCode: s.rowLabel,
  };
}

// ---------------------------------------------------------------- AUTH
export async function loginKeycloak(username, password) {
  const data = await tokenRequest({ grant_type: 'password', username, password });
  saveTokens(data);
  return data;
}

/** Keycloak không có API đăng ký công khai: chuyển sang trang đăng ký của realm (registrationAllowed = true). */
export function getRegisterUrl() {
  const params = new URLSearchParams({
    client_id: KC_CLIENT,
    response_type: 'code',
    scope: 'openid',
    redirect_uri: `${window.location.origin}/login`,
  });
  return `${KC_BASE}/realms/${KC_REALM}/protocol/openid-connect/registrations?${params}`;
}

export async function registerAccount() {
  window.location.href = getRegisterUrl();
  return new Promise(() => {});
}

// ---------------------------------------------------------------- MOVIES (movie-service)
export async function getMovies(status = 'NOW_SHOWING', { q, page = 0, size = 20 } = {}) {
  const data = await request('/movies', { query: { status, q, page, size } });
  return itemsOf(data).map(normalizeMovie);
}

export async function getMovieDetails(movieId) {
  return normalizeMovie(await request(`/movies/${movieId}`));
}

export async function getGenres() {
  return request('/movies/genres');
}

export async function createMovie(movieData) {
  return request('/movies', { method: 'POST', body: movieData });
}

export async function updateMovie(movieId, movieData) {
  return request(`/movies/${movieId}`, { method: 'PUT', body: movieData });
}

export async function archiveMovie(movieId) {
  return request(`/movies/${movieId}`, { method: 'DELETE' });
}

// ---------------------------------------------------------------- SEARCH (search-service, Elasticsearch)
/**
 * GET /api/v1/search/movies — full-text + facet + highlight trên Elasticsearch.
 * Trả nguyên MovieSearchResult: { items, page, size, totalElements, totalPages, facets, tookMs }.
 */
export async function searchMoviesES({
  q, person, genres, status, ageRating, language, country,
  releaseFrom, releaseTo, durationMin, durationMax, sort, page = 0, size = 12,
} = {}) {
  const data = await request('/search/movies', {
    query: {
      q, person, genres, status, ageRating, language, country,
      releaseFrom, releaseTo, durationMin, durationMax, sort, page, size,
    },
  });
  return {
    ...data,
    items: (data?.items || []).map((hit) => ({ ...normalizeMovie(hit), highlight: hit.highlight || {} })),
    facets: data?.facets || {},
  };
}

/** Giữ tên cũ: tìm nhanh theo từ khoá, trả về danh sách phim. */
export async function searchMovies(q) {
  return (await searchMoviesES({ q })).items;
}

/** GET /api/v1/search/movies/suggest — gợi ý khi đang gõ (completion/prefix). */
export async function suggestMovies(q, size = 6) {
  if (!q || !q.trim()) return [];
  return request('/search/movies/suggest', { query: { q, size } });
}

// ---------------------------------------------------------------- CINEMAS & SCREENS (cinema-service)
export async function getCinemas(city, { q, page = 0, size = 50 } = {}) {
  const data = await request('/cinemas', { query: { city, q, page, size } });
  return itemsOf(data);
}

export async function getCinemaById(cinemaId) {
  return request(`/cinemas/${cinemaId}`);
}

export async function getCinemaScreens(cinemaId) {
  return request(`/cinemas/${cinemaId}/screens`);
}

export async function createCinema(cinemaData) {
  return request('/cinemas', { method: 'POST', body: cinemaData });
}

export async function createScreen(cinemaId, screenData) {
  return request(`/cinemas/${cinemaId}/screens`, { method: 'POST', body: screenData });
}

// ---------------------------------------------------------------- SHOWTIMES (showtime-service)
/** Khách chỉ nhận suất SCHEDULED/SELLING sắp chiếu; tham số status chỉ có tác dụng với ADMIN. */
export async function getShowtimes(movieId, { cinemaId, date, status, page = 0, size = 100 } = {}) {
  const data = await request('/showtimes', { query: { movieId, cinemaId, date, status, page, size } });
  return itemsOf(data).map(normalizeShowtime);
}

export async function getShowtimeById(showtimeId) {
  return normalizeShowtime(await request(`/showtimes/${showtimeId}`));
}

export async function getShowtimeSeats(showtimeId) {
  return (await request(`/showtimes/${showtimeId}/seats`)).map(normalizeSeat);
}

export async function createShowtime(showtimeData) {
  return request('/showtimes', { method: 'POST', body: showtimeData });
}

export async function cancelShowtime(showtimeId) {
  return request(`/showtimes/${showtimeId}`, { method: 'DELETE' });
}

// ---------------------------------------------------------------- BOOKINGS (booking-service)
/** POST /api/v1/bookings — giữ ghế + tạo booking PENDING; payment được tạo bất đồng bộ qua Kafka. */
export async function createBooking(showtimeId, seats, showtime) {
  const body = {
    showtimeId,
    movieTitle: showtime?.movieTitle,
    cinemaName: showtime?.cinemaName,
    screenName: showtime?.screenName,
    startTime: showtime?.startAt,
    seats: seats.map((s) => ({
      showtimeSeatId: s.showtimeSeatId || s.id,
      seatId: s.seatId,
      seatLabel: s.seatLabel || s.seatCode,
      unitPrice: s.price,
    })),
  };
  const booking = await request('/bookings', { method: 'POST', body, idempotencyKey: newIdempotencyKey() });
  localStorage.setItem('pendingBookingId', booking.id);
  return booking;
}

export async function getUserBookings() {
  return request('/bookings');
}

export async function getBookingById(bookingId) {
  return request(`/bookings/${bookingId}`);
}

export async function getBookingByCode(bookingCode) {
  return request(`/bookings/code/${encodeURIComponent(bookingCode)}`);
}

export async function cancelBooking(bookingId, reason = 'Khách hàng yêu cầu hủy vé') {
  return request(`/bookings/${bookingId}/cancel`, { method: 'POST', body: { reason } });
}

export async function retryBookingPayment(bookingId) {
  return request(`/bookings/${bookingId}/payment-retry`, { method: 'POST' });
}

// ---------------------------------------------------------------- PAYMENTS (payment-service, PayOS)
export async function getPaymentByBooking(bookingId) {
  return request(`/payments/by-booking/${bookingId}`);
}

/** Hỏi lại PayOS trạng thái thật của payment mới nhất (không tin query string PayOS trả về). */
export async function syncPaymentByBooking(bookingId) {
  return request(`/payments/by-booking/${bookingId}/sync`, { method: 'POST' });
}

/** Tên cũ, giữ cho component cũ: giờ chỉ đồng bộ trạng thái thật từ PayOS, không còn giả lập. */
export const mockPayByBooking = syncPaymentByBooking;

/** Chờ payment được tạo (booking -> Kafka -> payment-service), tối đa ~timeoutMs. */
export async function waitForPayment(bookingId, { timeoutMs = 20000, intervalMs = 1500 } = {}) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const p = await getPaymentByBooking(bookingId);
      if (p) return p;
    } catch (e) {
      if (e.status && e.status !== 404) throw e;
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  return null;
}

/** Chờ booking ra trạng thái cuối (CONFIRMED / CANCELLED / EXPIRED...). */
export async function pollBookingUntilSettled(bookingId, { timeoutMs = 20000, intervalMs = 1500 } = {}) {
  const deadline = Date.now() + timeoutMs;
  let booking = await getBookingById(bookingId);
  while (['PENDING', 'CONFIRMING'].includes(booking.status) && Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, intervalMs));
    booking = await getBookingById(bookingId);
  }
  return booking;
}

// ---------------------------------------------------------------- STAFF / OPS
export async function getOutboxBacklog() {
  return request('/bookings/admin/outbox');
}

export async function requeueOutboxFailed() {
  return request('/bookings/admin/outbox/requeue-failed', { method: 'POST' });
}

export async function getPaymentDltStatus() {
  return request('/bookings/admin/payment-events-dlt');
}

export async function replayPaymentDlt() {
  return request('/bookings/admin/payment-events-dlt/replay', { method: 'POST' });
}

// ---------------------------------------------------------------- USERS (user-service)
export async function getCurrentUserProfile() {
  return request('/users/me');
}

export async function updateUserProfile(fullName, avatarUrl) {
  const user = await request('/users/me', { method: 'PUT', body: { fullName, avatarUrl } });
  if (avatarUrl) localStorage.setItem('userAvatar', avatarUrl);
  return user;
}

/** Trả PageResponse { items, ... } (AdminDashboard đọc data.items). */
export async function searchUsers(keyword = '', role = '', status = '', page = 0, size = 20) {
  return request('/users', { query: { q: keyword, role, status, page, size } });
}

export async function changeUserStatus(userId, status) {
  return request(`/users/${userId}/status`, { method: 'PATCH', body: { status } });
}
