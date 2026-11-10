import { ApiError } from '../api/client';
import { bookingsApi, paymentsApi } from '../api/endpoints';
import type { Booking, Payment } from '../api/types';
import { sleep } from './format';

/** PayOS return URL only carries orderCode, so the bookingId is kept here before leaving (FRONTEND_API §3.4) */
const KEY = 'mss301.pendingBookingId';
export const pendingBooking = {
  save: (id: string) => sessionStorage.setItem(KEY, id),
  get: () => sessionStorage.getItem(KEY),
  clear: () => sessionStorage.removeItem(KEY),
};

const hasLink = (p: Payment) => p.status === 'PENDING' && !!p.checkoutUrl;

/**
 * Poll GET /payments/by-booking/{id} until a PENDING payment with checkoutUrl appears (§3.4).
 * - 404 at first is normal (link is created asynchronously through Kafka)
 * - previousId: after payment-retry wait for a payment with a different id; if the old link is still
 *   open after `fallbackAfterMs`, use it (§3.6)
 */
export async function waitForPaymentLink(
  bookingId: string,
  opts: { previousId?: string; timeoutMs?: number; intervalMs?: number; fallbackAfterMs?: number; signal?: AbortSignal } = {},
): Promise<Payment | undefined> {
  const { previousId, timeoutMs = 30_000, intervalMs = 1500, fallbackAfterMs = 10_000, signal } = opts;
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    if (signal?.aborted) return undefined;
    try {
      const p = await paymentsApi.byBooking(bookingId);
      if (p.status === 'SUCCESS') return p;
      if (hasLink(p)) {
        if (!previousId || p.id !== previousId) return p;
        if (Date.now() - start > fallbackAfterMs) return p; // backend kept the old link
      }
    } catch (e) {
      if (!(e instanceof ApiError) || (e.status !== 404 && e.status < 500 && e.status !== 0)) throw e;
    }
    await sleep(intervalMs);
  }
  return undefined;
}

export type Outcome =
  | { kind: 'confirmed'; booking: Booking }
  | { kind: 'confirming'; booking: Booking; payment?: Payment }
  | { kind: 'unpaid'; booking: Booking; payment?: Payment }
  | { kind: 'failed'; booking: Booking; payment?: Payment; paidButFailed: boolean };

/** Decision table of FRONTEND_API §3.5: look at BOTH payment and booking status */
export function decide(booking: Booking, payment?: Payment): Outcome {
  if (booking.status === 'CONFIRMED') return { kind: 'confirmed', booking };
  if (booking.status === 'CANCELLED' || booking.status === 'EXPIRED') {
    return { kind: 'failed', booking, payment, paidButFailed: payment?.status === 'SUCCESS' };
  }
  if (payment?.status === 'SUCCESS' || booking.status === 'CONFIRMING') return { kind: 'confirming', booking, payment };
  return { kind: 'unpaid', booking, payment };
}

/** Poll the booking every 2s (max ~60s) while the outcome is "confirming" */
export async function pollBookingUntilSettled(
  bookingId: string,
  payment: Payment | undefined,
  onUpdate: (o: Outcome) => void,
  signal?: AbortSignal,
  timeoutMs = 60_000,
): Promise<Outcome> {
  const start = Date.now();
  let outcome = decide(await bookingsApi.get(bookingId), payment);
  onUpdate(outcome);
  while (outcome.kind === 'confirming' && Date.now() - start < timeoutMs && !signal?.aborted) {
    await sleep(2000);
    outcome = decide(await bookingsApi.get(bookingId), payment);
    onUpdate(outcome);
  }
  return outcome;
}
