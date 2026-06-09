/**
 * fetch() with a hard timeout. Every upstream call in enrichment is best-effort, so a slow or
 * hanging provider must abort on its own clock instead of stalling the whole serverless function
 * until Vercel's 60s ceiling (which surfaces to users as a 504). On timeout the underlying
 * AbortController fires, fetch rejects, and the caller's try/catch turns it into an empty result.
 */
export async function fetchWithTimeout(
  input: string,
  init: RequestInit & { timeoutMs?: number } = {},
): Promise<Response> {
  const { timeoutMs = 15_000, signal, ...rest } = init;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  // Respect a caller-supplied signal too: abort ours if theirs fires.
  if (signal) signal.addEventListener("abort", () => controller.abort(), { once: true });
  try {
    return await fetch(input, { ...rest, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}
