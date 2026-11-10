import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { ApiError } from '../api/client';
import { label } from '../utils/format';

export function StatusBadge({ value }: { value?: string | null }) {
  return <span className={`badge badge-${(value ?? '').toLowerCase()}`}>{label(value)}</span>;
}

/** Shows `message` and per-field `errors` from the API error body (FRONTEND_API §1.3 / §1.6) */
export function ErrorBox({ error }: { error?: unknown }) {
  if (!error) return null;
  const e = error as ApiError;
  return (
    <div className="error">
      <strong>{e.message || 'Đã có lỗi xảy ra'}</strong>
      {e.errors && (
        <ul>
          {Object.entries(e.errors).map(([k, v]) => (
            <li key={k}>
              <code>{k}</code>: {v}
            </li>
          ))}
        </ul>
      )}
      {e.retryAfter && <div className="muted">Vui lòng thử lại sau {e.retryAfter} giây.</div>}
      {e.correlationId && <div className="muted small">Mã tra cứu: {e.correlationId}</div>}
    </div>
  );
}

export function FieldError({ error, field }: { error?: unknown; field: string }) {
  const msg = (error as ApiError | undefined)?.errors?.[field];
  return msg ? <div className="field-error">{msg}</div> : null;
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button disabled={page <= 0} onClick={() => onChange(page - 1)}>‹ Trước</button>
      <span>Trang {page + 1} / {totalPages}</span>
      <button disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Sau ›</button>
    </div>
  );
}

export function Loading({ text = 'Đang tải…' }: { text?: string }) {
  return <p className="muted">{text}</p>;
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="ghost" onClick={onClose}>✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

/** Small data-loading hook: { data, error, loading, reload } */
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      setData(await run());
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, [run]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload, setData };
}

/** Wraps an action: disables the button while running, captures the error */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const run = async <T,>(fn: () => Promise<T>): Promise<T | undefined> => {
    setBusy(true);
    setError(undefined);
    try {
      return await fn();
    } catch (e) {
      setError(e);
      return undefined;
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, setError, run };
}
