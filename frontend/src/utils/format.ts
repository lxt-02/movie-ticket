const TZ = 'Asia/Ho_Chi_Minh';

/** API times are UTC ISO-8601; always display in Vietnam time */
export function fmtDateTime(iso?: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', { timeZone: TZ, dateStyle: 'short', timeStyle: 'short' });
}
export function fmtTime(iso?: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString('vi-VN', { timeZone: TZ, hour: '2-digit', minute: '2-digit' });
}
export function fmtDate(isoDate?: string | null) {
  if (!isoDate) return '—';
  return new Date(isoDate.length === 10 ? isoDate + 'T00:00:00Z' : isoDate).toLocaleDateString('vi-VN', { timeZone: isoDate.length === 10 ? 'UTC' : TZ });
}
export function fmtMoney(v?: number | null, currency = 'VND') {
  if (v === undefined || v === null) return '—';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(v);
}

/** YYYY-MM-DD of "today + offset" in Vietnam time (showtime `date` param is a Vietnam day) */
export function vnDate(offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * 86_400_000);
  return d.toLocaleDateString('en-CA', { timeZone: TZ });
}

/** datetime-local value (interpreted as Vietnam time) -> UTC ISO string */
export function vnLocalToIso(local: string) {
  // local = "2026-11-12T19:30"; Vietnam is UTC+7 with no DST
  return new Date(local + ':00+07:00').toISOString();
}
/** UTC ISO -> datetime-local value in Vietnam time */
export function isoToVnLocal(iso: string) {
  const d = new Date(new Date(iso).getTime() + 7 * 3_600_000);
  return d.toISOString().slice(0, 16);
}

export function uuid() {
  return crypto.randomUUID();
}

export const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export const LABELS: Record<string, string> = {
  COMING_SOON: 'Sắp chiếu',
  NOW_SHOWING: 'Đang chiếu',
  ENDED: 'Đã chiếu',
  ARCHIVED: 'Lưu trữ',
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Ngừng',
  CLOSED: 'Đóng cửa',
  MAINTENANCE: 'Bảo trì',
  BROKEN: 'Hỏng',
  SCHEDULED: 'Đã lên lịch',
  SELLING: 'Đang bán',
  CANCELLED: 'Đã huỷ',
  AVAILABLE: 'Trống',
  HELD: 'Đang giữ',
  BOOKED: 'Đã bán',
  BLOCKED: 'Bị khoá',
  PENDING: 'Chờ thanh toán',
  CONFIRMING: 'Đang xác nhận ghế',
  CONFIRMED: 'Thành công',
  EXPIRED: 'Hết hạn',
  PROCESSING: 'Đang xử lý',
  SUCCESS: 'Đã thanh toán',
  FAILED: 'Thất bại',
  CUSTOMER: 'Khách hàng',
  STAFF: 'Nhân viên',
  ADMIN: 'Quản trị',
};
export const label = (v?: string | null) => (v ? LABELS[v] ?? v : '—');

/** Escape HTML but keep the <em> tags Elasticsearch adds around matches */
export function safeHighlight(fragment: string) {
  const escaped = fragment.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  return escaped.replace(/&lt;(\/?)em&gt;/g, '<$1em>');
}
