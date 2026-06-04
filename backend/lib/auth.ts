/** Single-user bearer-token auth. Returns null when authorized, or an error response. */
import { NextResponse } from "next/server";

export function requireAuth(req: Request): NextResponse | null {
  const expected = process.env.APP_API_SECRET;
  if (!expected) {
    return NextResponse.json(
      { error: "Server misconfigured: APP_API_SECRET is not set" },
      { status: 500 },
    );
  }
  const header = req.headers.get("authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7).trim() : "";
  if (token !== expected) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  return null;
}
