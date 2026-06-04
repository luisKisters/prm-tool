import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { transcribe } from "@/lib/groq";
import { findLinkedIn, findAvatar, findCompanyDomain } from "@/lib/serper";
import { summarizeNote } from "@/lib/openrouter";
import type { EnrichResult, EnrichedTag } from "@/lib/types";

export const runtime = "nodejs";
export const maxDuration = 60;

/**
 * Read-only enrichment. Transcribes the voice memo, finds the LinkedIn profile + avatar +
 * company domain, and summarizes the note. Performs NO external writes — the app reviews this
 * result and later calls /api/commit to persist it.
 */
export async function POST(req: Request) {
  const unauthorized = requireAuth(req);
  if (unauthorized) return unauthorized;

  let form: FormData;
  try {
    form = await req.formData();
  } catch {
    return NextResponse.json({ error: "Expected multipart/form-data" }, { status: 400 });
  }

  const str = (k: string) => (form.get(k)?.toString() ?? "").trim();
  const prmId = str("clientId");
  if (!prmId) {
    return NextResponse.json({ error: "clientId is required" }, { status: 400 });
  }

  const firstName = str("firstName");
  const lastName = str("lastName");
  const company = str("company");
  const note = str("note");
  const fullName = [firstName, lastName].filter(Boolean).join(" ").trim();

  // Transcribe first (the summary depends on it), then enrich + summarize in parallel.
  const voice = form.get("voice");
  const transcript = voice instanceof File ? await transcribe(voice) : "";
  const fullNote = [note, transcript && `Voice transcript: ${transcript}`]
    .filter(Boolean)
    .join("\n\n");

  const [linkedin, avatarUrl, companyDomain, summary] = await Promise.all([
    fullName ? findLinkedIn(fullName, company) : Promise.resolve({ linkedinUrl: "", headline: "" }),
    fullName ? findAvatar(fullName, company) : Promise.resolve(""),
    findCompanyDomain(company),
    summarizeNote(fullNote),
  ]);

  const enriched: EnrichedTag[] = [];
  if (linkedin.linkedinUrl) enriched.push("LINKEDIN");
  if (linkedin.headline) enriched.push("JOB_TITLE");
  if (avatarUrl) enriched.push("AVATAR");
  if (companyDomain) enriched.push("COMPANY");

  const result: EnrichResult = {
    prmId,
    transcript,
    linkedinUrl: linkedin.linkedinUrl,
    headline: linkedin.headline,
    avatarUrl,
    companyDomain,
    summary,
    enriched,
  };
  return NextResponse.json(result);
}
