/** The public origin of the current request (honors Vercel's proxy headers). */
export function originOf(req: Request): string {
  const proto = req.headers.get("x-forwarded-proto") ?? "https";
  const host =
    req.headers.get("x-forwarded-host") ?? req.headers.get("host") ?? new URL(req.url).host;
  return `${proto}://${host}`;
}

/** Absolute Google OAuth callback URL for this deployment. Must match the registered redirect URI. */
export function googleCallbackUrl(req: Request): string {
  return `${originOf(req)}/api/google/callback`;
}
