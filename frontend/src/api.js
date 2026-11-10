const API_BASE = 'http://localhost:8080/api/v1';
const KC_URL = 'http://localhost:8180/realms/mss301/protocol/openid-connect/token';

// Default mock customer user ID (matches sample customer user in database)
export const DEFAULT_CUSTOMER_ID = '10000000-0000-0000-0000-000000000001';

export const getAuthToken = () => localStorage.getItem('token');
export const setAuthToken = (token) => localStorage.setItem('token', token);
export const getUserId = () => localStorage.getItem('userId') || null;
export const setUserId = (id) => localStorage.setItem('userId', id);
export const getUserRole = () => localStorage.getItem('userRole') || null;
export const isAuthenticated = () => !!localStorage.getItem('token');

export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

const getHeaders = (idempotencyKey = null) => {
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': getUserId(),
    'X-User-Role': getUserRole(),
  };
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  return headers;
};

// --- AUTH ---
export async function loginKeycloak(username = 'customer', password = '123456') {
  try {
    const params = new URLSearchParams();
    params.append('grant_type', 'password');
    params.append('client_id', 'mss301-frontend');
    params.append('username', username);
    params.append('password', password);

    const res = await fetch(KC_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params,
    });
    if (!res.ok) {
      const errJson = await res.json().catch(() => ({}));
      throw new Error(errJson.error_description || 'Tài khoản hoặc mật khẩu không chính xác');
    }
    const data = await res.json();
    setAuthToken(data.access_token);

    const payload = parseJwt(data.access_token);
    if (payload) {
      if (payload.sub) setUserId(payload.sub);
      if (payload.preferred_username) localStorage.setItem('username', payload.preferred_username);
      if (payload.email) localStorage.setItem('userEmail', payload.email);
      const roles = payload.realm_access?.roles || [];
      let role = 'CUSTOMER';
      if (roles.includes('ADMIN')) role = 'ADMIN';
      else if (roles.includes('STAFF')) role = 'STAFF';
      localStorage.setItem('userRole', role);
    } else {
      localStorage.setItem('username', username);
      localStorage.setItem('userRole', username.toUpperCase());
    }
    return data;
  } catch (err) {
    console.warn('Keycloak login error:', err.message);
    // Dev fallback if Keycloak is temporarily paused
    if (err.message.includes('Failed to fetch') || err.message.includes('offline')) {
      const mockId = username === 'admin'
        ? '10000000-0000-0000-0000-000000000004'
        : username === 'staff'
        ? '10000000-0000-0000-0000-000000000002'
        : username === 'blocked'
        ? '10000000-0000-0000-0000-000000000003'
        : DEFAULT_CUSTOMER_ID;
      setUserId(mockId);
      setAuthToken('mock_token_' + username);
      localStorage.setItem('username', username);
      localStorage.setItem('userRole', username.toUpperCase());
      return { access_token: 'mock_token_' + username };
    }
    throw err;
  }
}

// --- ADMIN MOVIE CRUD ---
export async function createMovie(movieData) {
  const res = await fetch(`${API_BASE}/movies`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(movieData),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Thêm phim mới thất bại');
  return json.data || json;
}

export async function updateMovie(movieId, movieData) {
  const res = await fetch(`${API_BASE}/movies/${movieId}`, {
    method: 'PUT',
    headers: getHeaders(),
    body: JSON.stringify(movieData),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Cập nhật phim thất bại');
  return json.data || json;
}

export async function archiveMovie(movieId) {
  const res = await fetch(`${API_BASE}/movies/${movieId}`, {
    method: 'DELETE',
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Xóa/Lưu trữ phim thất bại');
  return json.data || json;
}

// --- ADMIN CINEMA & SCREEN CRUD ---
export async function createCinema(cinemaData) {
  const res = await fetch(`${API_BASE}/cinemas`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(cinemaData),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Thêm rạp mới thất bại');
  return json.data || json;
}

export async function createScreen(cinemaId, screenData) {
  const res = await fetch(`${API_BASE}/cinemas/${cinemaId}/screens`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(screenData),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Thêm phòng chiếu thất bại');
  return json.data || json;
}

export async function getCinemaScreens(cinemaId) {
  try {
    const res = await fetch(`${API_BASE}/cinemas/${cinemaId}/screens`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Fetch screens error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return [
      { id: '32000000-0000-0000-0000-000000000001', name: 'Phòng Chiếu 1 (IMAX Laser)', screenType: 'IMAX' },
      { id: '32000000-0000-0000-0000-000000000002', name: 'Phòng Chiếu 2 (Dolby Atmos)', screenType: 'STANDARD' },
    ];
  }
}

// --- SHOWTIME & BOOKING CRUD ---
export async function createShowtime(showtimeData) {
  const res = await fetch(`${API_BASE}/showtimes`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(showtimeData),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Tạo suất chiếu mới thất bại');
  return json.data || json;
}

export async function cancelShowtime(showtimeId) {
  const res = await fetch(`${API_BASE}/showtimes/${showtimeId}`, {
    method: 'DELETE',
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Hủy suất chiếu thất bại');
  return json.data || json;
}

export async function cancelBooking(bookingId) {
  const res = await fetch(`${API_BASE}/bookings/${bookingId}/cancel`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ reason: 'Khách hàng yêu cầu hủy vé' }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Hủy vé thất bại');
  return json.data || json;
}

export async function getBookingById(bookingId) {
  const res = await fetch(`${API_BASE}/bookings/${bookingId}`, {
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Không tìm thấy thông tin vé');
  return json.data || json;
}

export function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('userEmail');
  localStorage.removeItem('userRole');
  localStorage.removeItem('userId');
  localStorage.removeItem('userAvatar');
  localStorage.removeItem('userFullName');
}

// --- REGISTER ---
export async function registerAccount({ fullName, email, username, password, phone }) {
  // Try Keycloak or mock user creation
  try {
    const res = await fetch(`http://localhost:8180/realms/mss301/protocol/openid-connect/registrations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, username, password }),
    });
    if (res.ok) {
      return await loginKeycloak(username, password);
    }
  } catch (e) {
    console.warn('Direct KC registration fallback:', e);
  }

  // Fallback demo account generation
  const newId = '10000000-0000-0000-' + Math.random().toString(16).substring(2, 6) + '-' + Date.now().toString().slice(-12);
  setUserId(newId);
  setAuthToken('mock_reg_token_' + Date.now());
  localStorage.setItem('username', username);
  localStorage.setItem('userEmail', email);
  localStorage.setItem('userRole', 'CUSTOMER');
  localStorage.setItem('userFullName', fullName || username);
  return { access_token: 'mock_token', userId: newId };
}

// --- STAFF OPERATIONS ---
export async function getBookingByCode(bookingCode) {
  const res = await fetch(`${API_BASE}/bookings/code/${encodeURIComponent(bookingCode)}`, {
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || 'Không tìm thấy vé với mã này');
  }
  return json.data || json;
}

export async function getOutboxBacklog() {
  try {
    const res = await fetch(`${API_BASE}/bookings/admin/outbox`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Cannot fetch outbox status');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return { pending: 0, failed: [], paused: false };
  }
}

export async function requeueOutboxFailed() {
  const res = await fetch(`${API_BASE}/bookings/admin/outbox/requeue-failed`, {
    method: 'POST',
    headers: getHeaders(),
  });
  return await res.json();
}

export async function getPaymentDltStatus() {
  try {
    const res = await fetch(`${API_BASE}/bookings/admin/payment-events-dlt`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Cannot fetch DLT status');
    return await res.json();
  } catch (e) {
    return { replayRunning: false, chaosEnabled: false };
  }
}

export async function replayPaymentDlt() {
  const res = await fetch(`${API_BASE}/bookings/admin/payment-events-dlt/replay`, {
    method: 'POST',
    headers: getHeaders(),
  });
  return await res.json();
}

// --- USER PROFILE & ADMIN ---
export async function getCurrentUserProfile() {
  try {
    const res = await fetch(`${API_BASE}/users/me`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Fetch profile failed');
    const json = await res.json();
    return json.data || json;
  } catch (err) {
    return {
      id: getUserId(),
      fullName: localStorage.getItem('username') === 'admin' ? 'Demo Admin' : 'Demo Customer',
      email: localStorage.getItem('userEmail') || `${localStorage.getItem('username') || 'customer'}@example.com`,
      avatarUrl: localStorage.getItem('userAvatar') || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
      role: getUserRole(),
      status: 'ACTIVE',
    };
  }
}

export async function updateUserProfile(fullName, avatarUrl) {
  const res = await fetch(`${API_BASE}/users/me`, {
    method: 'PUT',
    headers: getHeaders(),
    body: JSON.stringify({ fullName, avatarUrl }),
  });
  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || 'Cập nhật hồ sơ thất bại');
  }
  if (avatarUrl) localStorage.setItem('userAvatar', avatarUrl);
  return json.data || json;
}

export async function searchUsers(keyword = '', role = '', status = '', page = 0, size = 20) {
  let url = `${API_BASE}/users?page=${page}&size=${size}`;
  if (keyword) url += `&q=${encodeURIComponent(keyword)}`;
  if (role) url += `&role=${encodeURIComponent(role)}`;
  if (status) url += `&status=${encodeURIComponent(status)}`;

  const res = await fetch(url, {
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || 'Chỉ ADMIN mới có quyền xem danh sách người dùng!');
  }
  return json.data || json;
}

export async function changeUserStatus(userId, status) {
  const res = await fetch(`${API_BASE}/users/${userId}/status`, {
    method: 'PATCH',
    headers: getHeaders(),
    body: JSON.stringify({ status }),
  });
  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || 'Thay đổi trạng thái tài khoản thất bại');
  }
  return json.data || json;
}

// --- MOVIES ---
export async function getMovies(status = 'NOW_SHOWING') {
  try {
    const res = await fetch(`${API_BASE}/movies?status=${status}&page=0&size=20`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Fetch movies error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    console.warn('API error, using sample movies:', e);
    return getSampleMovies();
  }
}

export async function searchMovies(q) {
  try {
    const res = await fetch(`${API_BASE}/search/movies?q=${encodeURIComponent(q)}&page=0&size=12`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Search movies error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return getSampleMovies().filter(m => m.title.toLowerCase().includes(q.toLowerCase()));
  }
}

export async function getMovieDetails(movieId) {
  try {
    const res = await fetch(`${API_BASE}/movies/${movieId}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Get movie detail error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return getSampleMovies().find(m => m.id === movieId) || getSampleMovies()[0];
  }
}

// --- CINEMAS & SHOWTIMES ---
export async function getCinemas(city = 'Ho Chi Minh City') {
  try {
    const res = await fetch(`${API_BASE}/cinemas?city=${encodeURIComponent(city)}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Get cinemas error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return [
      { id: '30000000-0000-0000-0000-000000000001', name: 'CineStar Quốc Thanh', address: '271 Nguyễn Trãi, Q.1, TP.HCM', city: 'Ho Chi Minh City' },
      { id: '30000000-0000-0000-0000-000000000002', name: 'CineStar Hai Bà Trưng', address: '135 Hai Bà Trưng, Q.3, TP.HCM', city: 'Ho Chi Minh City' },
    ];
  }
}

export async function getShowtimes(movieId, status = 'SELLING') {
  try {
    const res = await fetch(`${API_BASE}/showtimes?movieId=${movieId}&status=${status}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Get showtimes error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return [
      {
        id: '50000000-0000-0000-0000-000000000001',
        movieId: movieId,
        screenId: '32000000-0000-0000-0000-000000000001',
        cinemaName: 'CineStar Quốc Thanh',
        screenName: 'Phòng Chiếu 1 (IMAX)',
        startTime: '19:30',
        date: 'Hôm nay',
        basePrice: 95000,
        status: 'SELLING',
      },
      {
        id: '50000000-0000-0000-0000-000000000002',
        movieId: movieId,
        screenId: '32000000-0000-0000-0000-000000000001',
        cinemaName: 'CineStar Quốc Thanh',
        screenName: 'Phòng Chiếu 1 (IMAX)',
        startTime: '21:45',
        date: 'Hôm nay',
        basePrice: 95000,
        status: 'SELLING',
      },
    ];
  }
}

// --- SEAT LAYOUT & REALTIME STATUS ---
export async function getShowtimeSeats(showtimeId) {
  try {
    const res = await fetch(`${API_BASE}/showtimes/${showtimeId}/seats`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Get seats error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return getSampleSeats();
  }
}

// --- BOOKING ---
export async function createBooking(showtimeId, seatsInput) {
  const idempotencyKey = 'bk_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
  
  // Match backend CreateBookingRequest & CreateBookingSeatRequest DTOs
  const formattedSeats = Array.isArray(seatsInput)
    ? seatsInput.map((s) => {
        if (typeof s === 'string') {
          return {
            showtimeSeatId: s,
            seatLabel: 'Ghế',
            unitPrice: 95000,
          };
        }
        return {
          showtimeSeatId: s.showtimeSeatId || s.id,
          seatId: s.seatId || null,
          seatLabel: s.seatLabel || s.seatCode || 'Ghế',
          unitPrice: s.unitPrice || s.price || 95000,
        };
      })
    : [];

  const body = {
    showtimeId,
    seats: formattedSeats,
  };

  const res = await fetch(`${API_BASE}/bookings`, {
    method: 'POST',
    headers: getHeaders(idempotencyKey),
    body: JSON.stringify(body),
  });

  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || json.error || 'Đặt vé thất bại! Ghế có thể đã bị giữ hoặc tài khoản bị khóa.');
  }
  return json.data || json;
}

export async function getUserBookings() {
  try {
    const res = await fetch(`${API_BASE}/bookings`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Get bookings error');
    const json = await res.json();
    return json.data || json;
  } catch (e) {
    return [];
  }
}

// --- PAYMENT & PAYOS SYNC ---
export async function getPaymentByBooking(bookingId) {
  const res = await fetch(`${API_BASE}/payments/by-booking/${bookingId}`, {
    headers: getHeaders(),
  });
  if (!res.ok) throw new Error('Payment not yet ready');
  const json = await res.json();
  return json.data || json;
}

export async function syncPaymentByBooking(bookingId) {
  const res = await fetch(`${API_BASE}/payments/by-booking/${bookingId}/sync`, {
    method: 'POST',
    headers: getHeaders(),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Đồng bộ trạng thái PayOS thất bại');
  return json.data || json;
}

export async function mockPayByBooking(bookingId) {
  try {
    const res = await fetch(`${API_BASE}/payments/by-booking/${bookingId}/mock-pay`, {
      method: 'POST',
      headers: getHeaders(),
    });
    const json = await res.json();
    if (res.ok) return json.data || json;
  } catch (e) {
    // If backend only has PayOS webhook/sync without mock-pay endpoint, trigger sync
    try {
      return await syncPaymentByBooking(bookingId);
    } catch (syncErr) {
      // Demo fallback return
      return { status: 'PAID', amount: 190000, bookingId };
    }
  }
  return { status: 'PAID', amount: 190000, bookingId };
}

// Sample fallback movies with CGV authentic lineup
function getSampleMovies() {
  return [
    {
      id: '20000000-0000-0000-0000-000000000010',
      title: 'BÒ SỮA BAY',
      originalTitle: 'Flying Dairy Cow',
      durationMinutes: 115,
      releaseDate: '2026-11-06',
      ageRating: 'P',
      genres: ['Hài', 'Phiêu Lưu', 'Gia Đình'],
      posterUrl: 'https://images.unsplash.com/photo-1500595046743-cd271d694d30?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1500595046743-cd271d694d30?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Dự án phim điện ảnh đặc sắc được tuyển chọn APM & DANAFF. Câu chuyện hài hước, xúc động về hành trình theo đuổi ước mơ kỳ lạ của nhóm bạn trẻ nông thôn cùng chú bò sữa đặc biệt.',
      director: 'Nguyễn Phạm Thành Đạt',
      actors: 'Nguyễn Hùng, Ngọc Xuân, Võ Điền Gia Huy, Tiểu Vy, Huy Tít',
      format: '2D Digital • IMAX',
    },
    {
      id: '20000000-0000-0000-0000-000000000011',
      title: 'THIÊN ĐƯỜNG TĂM TỐI',
      originalTitle: 'Dark Paradise',
      durationMinutes: 128,
      releaseDate: '2026-11-06',
      ageRating: 'T18',
      genres: ['Tâm Lý', 'Giật Gân', 'Kinh Dị'],
      posterUrl: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Tác phẩm điện ảnh tâm lý hồi hộp nghẹt thở. Một kỳ nghỉ định mệnh mở ra bí mật kinh hoàng đằng sau vẻ hào nhoáng của khu nghỉ dưỡng biệt lập.',
      director: 'Trần Bửu Lộc',
      actors: 'Quang Tuấn, Hoàng Yến Chibi, Quốc Cường',
      format: '2D • 4DX',
    },
    {
      id: '20000000-0000-0000-0000-000000000012',
      title: 'VENOM: KÈO CUỐI',
      originalTitle: 'Venom: The Last Dance',
      durationMinutes: 110,
      releaseDate: '2026-10-25',
      ageRating: 'T16',
      genres: ['Hành Động', 'Khoa Học Viễn Tưởng', 'Kỳ Ảo'],
      posterUrl: 'https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1563089145-599997674d42?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Eddie Brock và Venom đứng trước ngã rẽ sinh tử khi bị cả thế giới loài người và chủng tộc nguyên bản săn đuổi trong cuộc chạm trán định mệnh cuối cùng.',
      director: 'Kelly Marcel',
      actors: 'Tom Hardy, Chiwetel Ejiofor, Juno Temple',
      format: 'IMAX • 4DX • ScreenX',
    },
    {
      id: '20000000-0000-0000-0000-000000000013',
      title: 'CÔ DÂU HÀO MÔN',
      originalTitle: 'The Rich Bride',
      durationMinutes: 114,
      releaseDate: '2026-10-18',
      ageRating: 'T16',
      genres: ['Hài', 'Tâm Lý', 'Gia Đình'],
      posterUrl: 'https://images.unsplash.com/photo-1519741497674-611481863552?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1519741497674-611481863552?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Một phi vụ "phông bạt" tinh vi để bước chân vào giới thượng lưu của một gia đình bình dân nhưng bất ngờ vướng vào những toan tính không ngờ.',
      director: 'Vũ Ngọc Đãng',
      actors: 'Uyển Ân, Thu Trang, Kiều Minh Tuấn, Lê Giang',
      format: '2D Digital',
    },
    {
      id: '20000000-0000-0000-0000-000000000001',
      title: 'AVATAR: DÒNG CHẢY CỦA NƯỚC',
      originalTitle: 'Avatar: The Way of Water',
      durationMinutes: 192,
      releaseDate: '2026-10-15',
      ageRating: 'T13',
      genres: ['Hành Động', 'Khoa Học Viễn Tưởng', 'Phiêu Lưu'],
      posterUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Lấy bối cảnh hơn một thập kỷ sau những sự kiện của phần phim đầu tiên, bộ phim kể câu chuyện về gia đình Sully và những hiểm nguy họ phải đối mặt để bảo vệ lẫn nhau.',
      director: 'James Cameron',
      actors: 'Sam Worthington, Zoe Saldana, Sigourney Weaver',
      format: 'IMAX 3D',
    },
    {
      id: '20000000-0000-0000-0000-000000000002',
      title: 'LẬT MẶT 7: MỘT ĐIỀU ƯỚC',
      originalTitle: 'Face Off 7: One Wish',
      durationMinutes: 138,
      releaseDate: '2026-10-20',
      ageRating: 'T16',
      genres: ['Gia Đình', 'Chính Kịch', 'Hài'],
      posterUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80',
      bannerUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80',
      synopsis: 'Câu chuyện cảm động về tình mẫu tử của người mẹ già 73 tuổi cùng 5 người con đã trưởng thành. Mỗi người một hoàn cảnh và ước nguyện hướng về gia đình.',
      director: 'Lý Hải',
      actors: 'Thanh Hiền, Trương Minh Cường, Đinh Y Nhung',
      format: '2D Digital',
    },
  ];
}

// Sample fallback seats
function getSampleSeats() {
  const rows = ['A', 'B', 'C', 'D', 'E'];
  const seats = [];
  rows.forEach((r, rowIdx) => {
    for (let c = 1; c <= 8; c++) {
      const seatCode = `${r}${c}`;
      const isVip = rowIdx >= 2 && rowIdx <= 3;
      const isCouple = rowIdx === 4 && (c === 4 || c === 5);
      const isBooked = (r === 'A' && (c === 1 || c === 2)) || (r === 'C' && c === 4);
      const isHeld = r === 'B' && c === 3;
      seats.push({
        id: `51000000-0000-0000-0000-0000000000${rowIdx * 8 + c}`,
        seatCode,
        rowCode: r,
        seatNumber: c,
        seatType: isCouple ? 'COUPLE' : isVip ? 'VIP' : 'STANDARD',
        status: isBooked ? 'BOOKED' : isHeld ? 'HELD' : 'AVAILABLE',
        price: isCouple ? 190000 : isVip ? 115000 : 95000,
      });
    }
  });
  return seats;
}
