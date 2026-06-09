import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { extractTitle } from "@/lib/html";
import { toDomain } from "@/lib/serper";

export const runtime = "nodejs";
export const maxDuration = 20;

/**
 * Resolve a company domain to its website's <title>. Lets the review screen show what a domain
 * actually points at. Best-effort: returns an empty title when the site can't be fetched.
 */
export async function POST(req: Request) {
  const unauthorized = requireAuth(req);
  if (unauthorized) return unauthorized;

  let body: { domain?: string };
  try {
    body = (await req.json()) as { domain?: string };
  } catch {
    return NextResponse.json({ error: "Expected application/json" }, { status: 400 });
  }

  const domain = toDomain((body.domain ?? "").trim());
  if (!domain) return NextResponse.json({ domain: "", title: "" });

  let title = "";
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 8000);
    const res = await fetch(`https://${domain}`, {
      signal: controller.signal,
      redirect: "follow",
      headers: { "User-Agent": "Mozilla/5.0 (compatible; PRMToolBot/1.0)" },
    });
    clearTimeout(timer);
    if (res.ok) {
      // Only read the head; titles live early in the document.
      const html = (await res.text()).slice(0, 100_000);
      title = extractTitle(html);
    }
  } catch {
    // best-effort; leave title empty
  }

  return NextResponse.json({ domain, title });
}
