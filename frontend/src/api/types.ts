// Types mirror FRONTEND_API.md (gateway contract)

export type Role = 'CUSTOMER' | 'STAFF' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'BLOCKED';
export type MovieStatus = 'COMING_SOON' | 'NOW_SHOWING' | 'ENDED' | 'ARCHIVED';
export type CinemaStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED';
export type ScreenStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE';
export type SeatStatus = 'ACTIVE' | 'BROKEN' | 'INACTIVE';
export type ShowtimeStatus = 'SCHEDULED' | 'SELLING' | 'CANCELLED' | 'ENDED';
export type ShowtimeSeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED';
export type BookingStatus = 'PENDING' | 'CONFIRMING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED';
export type AgeRating = 'P' | 'K' | 'T13' | 'T16' | 'T18';

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string | null;
  role: Role;
  status: UserStatus;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Genre { id: string; name: string }
export interface Person { id: string; name: string; avatarUrl?: string | null; biography?: string | null }
export interface CrewMember { id: string; name: string; characterName?: string | null }

export interface Movie {
  id: string;
  title: string;
  originalTitle?: string | null;
  description?: string | null;
  durationMinutes: number;
  releaseDate?: string | null;
  ageRating?: AgeRating | null;
  language?: string | null;
  country?: string | null;
  posterUrl?: string | null;
  status: MovieStatus;
  genres: Genre[];
  directors: CrewMember[];
  cast: CrewMember[];
  createdAt: string;
  updatedAt: string;
}

export interface MovieRequest {
  title: string;
  originalTitle?: string;
  description?: string;
  durationMinutes: number;
  releaseDate?: string;
  ageRating?: AgeRating;
  language?: string;
  country?: string;
  posterUrl?: string;
  status?: MovieStatus;
  genreIds: string[];
  crew: { personId: string; role: 'DIRECTOR' | 'ACTOR'; characterName?: string }[];
}

export interface SearchHit {
  id: string;
  title: string;
  originalTitle?: string;
  description?: string;
  posterUrl?: string;
  genres: string[];
  directors: string[];
  actors: string[];
  ageRating?: string;
  language?: string;
  country?: string;
  status: MovieStatus;
  releaseDate?: string;
  durationMinutes?: number;
  score?: number;
  highlight?: Record<string, string[]>;
}
export interface Facet { value: string; count: number }
export interface SearchResult extends Page<SearchHit> {
  facets: Partial<Record<'genres' | 'status' | 'ageRating' | 'language' | 'country', Facet[]>>;
  tookMs: number;
}

export interface Cinema {
  id: string;
  name: string;
  brand?: string | null;
  address: string;
  city: string;
  district?: string | null;
  phone?: string | null;
  status: CinemaStatus;
  createdAt: string;
}
export type CinemaRequest = Omit<Cinema, 'id' | 'createdAt' | 'status'> & { status?: CinemaStatus };

export interface Screen {
  id: string;
  cinemaId: string;
  screenTypeId: string;
  screenTypeName: string;
  name: string;
  totalSeats: number;
  status: ScreenStatus;
}
export interface ScreenType { id: string; name: string; description?: string }
export interface SeatType { id: string; name: string; priceMultiplier: number }
export interface LayoutSeat {
  id: string;
  rowLabel: string;
  seatNumber: number;
  label: string;
  seatTypeId: string;
  seatTypeName: string;
  priceMultiplier: number;
  status: SeatStatus;
}
export interface ScreenLayout { screen: Screen; seats: LayoutSeat[] }
export interface LayoutRow { rowLabel: string; seatCount: number; seatTypeId: string }

export interface Showtime {
  id: string;
  movieId: string;
  screenId: string;
  movieTitle: string;
  cinemaId: string;
  cinemaName: string;
  screenName: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  status: ShowtimeStatus;
  createdAt: string;
}

export interface ShowtimeSeat {
  showtimeSeatId: string;
  seatId: string;
  seatLabel: string;
  rowLabel: string;
  seatNumber: number;
  seatType: string;
  price: number;
  status: ShowtimeSeatStatus;
}

export interface BookingSeat {
  id: string;
  showtimeSeatId: string;
  seatId: string;
  seatLabel: string;
  unitPrice: number;
  ticketCode?: string | null;
  status: string;
}

export interface Booking {
  id: string;
  bookingCode: string;
  userId: string;
  showtimeId: string;
  movieTitle: string;
  cinemaName: string;
  screenName: string;
  startTime: string;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  currency: string;
  status: BookingStatus;
  expiresAt: string;
  createdAt: string;
  confirmedAt?: string | null;
  cancelledAt?: string | null;
  paidPaymentId?: string | null;
  updatedAt: string;
  seats: BookingSeat[];
}

export interface Payment {
  id: string;
  bookingId: string;
  userId: string;
  paymentCode: string;
  provider: string;
  paymentMethod?: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  checkoutUrl?: string | null;
  qrCode?: string | null;
  payosOrderCode?: number | string | null;
  failureReason?: string | null;
  paidAt?: string | null;
  expiredAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Suggestion {
  id: string;
  title: string;
  originalTitle?: string | null;
  posterUrl?: string | null;
  releaseDate?: string | null;
  status: MovieStatus;
}
