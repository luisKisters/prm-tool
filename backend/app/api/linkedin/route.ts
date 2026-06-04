import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { lookupLinkedInProfile } from "@/lib/serper";

export const runtime = "nodejs";
export const maxDuration = 30;

/**
 * Re-derive a contact's headline + avatar from a specific LinkedIn URL. Called when the user edits
 * the LinkedIn field on the review screen. Read-only and best-effort: returns empty strings when
 * the profile can't be resolved (LinkedIn has no public API).
 */
export async function POST(req: Request) {
  const unauthorized = requireAuth(req);
  if (unauthorized) return unauthorized;

  let body: { url?: string; name?: string };
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Expected JSON" }, { status: 400 });
  }

  const url = (body.url ?? "").trim();
  if (!url) {
    return NextResponse.json({ error: "url is required" }, { status: 400 });
  }

  const { headline, avatarUrl } = await lookupLinkedInProfile(url, body.name ?? "");
  return NextResponse.json({ linkedinUrl: url, headline, avatarUrl });
}
