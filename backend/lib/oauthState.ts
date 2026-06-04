/**
 * Signed OAuth `state` so the Google callback can prove the flow was started by someone who
 * knew APP_API_SECRET — without ever putting the secret itself in a URL. Stateless: it's an
 * HMAC over a timestamp, verified on the way back.
 */
import { createHmac, timingSafeEqual } from "crypto";

const MAX_AGE_MS = 15 * 60 * 1000; // 15 minutes

function sign(ts: string, secret: string): string {
  return createHmac("sha256", secret).update(ts).digest("hex");
}

export function makeState(secret: string): string {
  const ts = Date.now().toString();
  return `${ts}.${sign(ts, secret)}`;
}

export function verifyState(state: string | null, secret: string): boolean {
  if (!state || !secret) return false;
  const [ts, mac] = state.split(".");
  if (!ts || !mac) return false;
  const age = Date.now() - Number(ts);
  if (!Number.isFinite(age) || age < 0 || age > MAX_AGE_MS) return false;

  const expected = sign(ts, secret);
  const a = Buffer.from(mac);
  const b = Buffer.from(expected);
  return a.length === b.length && timingSafeEqual(a, b);
}
