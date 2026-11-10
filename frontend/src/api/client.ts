import { config } from '../config';

/** Error shape returned by every service and the gateway (FRONTEND_API §1.3) */
export class ApiError extends Error {
  status: number;
  errors?: Record<string, string>;
  retryAfter?: number;
  correlationId?: string;

  constructor(status: number, message: string, errors?: Record<string, string>, retryAfter?: number, correlationId?: string) {
    super(message);
    this.status = status;
    this.errors = errors;
    this.retryAfter = retryAfter;
    this.correlationId = correlationId;
  }
}

type QueryValue = string | number | boolean | undefined | null | (string | number)[];
export type Query = Record<string, QueryValue>;

interface AuthHooks {
  /** returns a fresh access token (refreshing if needed) or undefined when anonymous */
  getToken: (forceRefresh?: boolean) => Promise<string | undefined>;
  /** called when the token cannot be refreshed */
  onUnauthorized: () => void;
}

let auth: AuthHooks = {
  getToken: async () => undefined,
  onUnauthorized: () => {},
};

export function setAuthHooks(hooks: AuthHooks) {
  auth = hooks;
}

function buildUrl(path: string, query?: Query) {
  const url = new URL(path, config.apiBaseUrl);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === '') continue;
      if (Array.isArray(value)) value.forEach((v) => url.searchParams.append(key, String(v)));
      else url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

export interface RequestOptions {
  query?: Query;
  body?: unknown;
  headers?: Record<string, string>;
}

async function send<T>(method: string, path: string, opts: RequestOptions = {}, retried = false): Promise<T> {
  const token = await auth.getToken(retried);
  const headers: Record<string, string> = { Accept: 'application/json', ...opts.headers };
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(buildUrl(path, opts.query), {
      method,
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    });
  } catch {
    // network error / gateway down: treat like 503 so callers can retry with the same Idempotency-Key
    throw new ApiError(0, 'Không kết nối được tới máy chủ. Vui lòng thử lại.');
  }

  if (res.status === 401 && token && !retried) {
    // token expired between refresh and request: force refresh once
    return send<T>(method, path, opts, true);
  }

  const text = await res.text();
  const json = text ? safeParse(text) : undefined;

  if (!res.ok || (json && json.success === false)) {
    if (res.status === 401) auth.onUnauthorized();
    const retryAfter = Number(res.headers.get('Retry-After')) || undefined;
    throw new ApiError(
      res.status,
      json?.message ?? defaultMessage(res.status),
      json?.errors ?? undefined,
      retryAfter,
      res.headers.get('X-Correlation-Id') ?? undefined,
    );
  }
  // success: { success, message, data, timestamp }
  return (json && 'data' in json ? json.data : json) as T;
}

function safeParse(text: string) {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function defaultMessage(status: number) {
  switch (status) {
    case 401: return 'Bạn cần đăng nhập';
    case 403: return 'Bạn không có quyền thực hiện thao tác này';
    case 404: return 'Không tìm thấy dữ liệu';
    case 429: return 'Bạn thao tác quá nhanh, vui lòng thử lại sau';
    case 503:
    case 504: return 'Dịch vụ đang tạm lỗi, vui lòng thử lại sau';
    default: return `Lỗi ${status}`;
  }
}

export const http = {
  get: <T>(path: string, query?: Query) => send<T>('GET', path, { query }),
  post: <T>(path: string, body?: unknown, headers?: Record<string, string>, query?: Query) =>
    send<T>('POST', path, { body, headers, query }),
  put: <T>(path: string, body?: unknown) => send<T>('PUT', path, { body }),
  patch: <T>(path: string, body?: unknown) => send<T>('PATCH', path, { body }),
  delete: <T>(path: string) => send<T>('DELETE', path),
};
