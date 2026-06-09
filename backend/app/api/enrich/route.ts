import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { transcribe } from "@/lib/groq";
import { findLinkedIn, findAvatar, findCompanyDomain, findCompanyDetails } from "@/lib/serper";
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

  // Everything runs concurrently. The LinkedIn/avatar/company lookups don't depend on the voice
  // transcript, so they fire immediately rather than waiting for Whisper. Only the summary needs
  // the transcript, so it's chained off transcription — the one genuine dependency.
  const voice = form.get("voice");
  const transcriptPromise = voice instanceof File ? transcribe(voice) : Promise.resolve("");

  const summaryPromise = transcriptPromise.then((transcript) => {
    const fullNote = [note, transcript && `Voice transcript: ${transcript}`]
      .filter(Boolean)
      .join("\n\n");
    return summarizeNote(fullNote);
  });

  // findCompanyDetails' network lookups key off the company *name* only (the domain arg merely
  // adds the DOMAIN tag), so it runs in this same concurrent block instead of waiting for the
  // domain. We fold the DOMAIN tag in afterwards once findCompanyDomain has resolved.
  const [transcript, linkedin, avatarUrl, companyDomain, summary, companyDetailsRaw] =
    await Promise.all([
      transcriptPromise,
      fullName ? findLinkedIn(fullName, company) : Promise.resolve({ linkedinUrl: "", headline: "" }),
      fullName ? findAvatar(fullName, company) : Promise.resolve(""),
      findCompanyDomain(company),
      summaryPromise,
      company
        ? findCompanyDetails(company, "")
        : Promise.resolve({ linkedinUrl: "", employees: null, address: "", enriched: [] as string[] }),
    ]);

  const enriched: EnrichedTag[] = [];
  if (linkedin.linkedinUrl) enriched.push("LINKEDIN");
  if (linkedin.headline) enriched.push("JOB_TITLE");
  if (avatarUrl) enriched.push("AVATAR");
  if (companyDomain) enriched.push("COMPANY");

  const companyDetails =
    companyDomain && !companyDetailsRaw.enriched.includes("DOMAIN")
      ? { ...companyDetailsRaw, enriched: ["DOMAIN", ...companyDetailsRaw.enriched] }
      : companyDetailsRaw;

  const result: EnrichResult = {
    prmId,
    transcript,
    email: "",
    linkedinUrl: linkedin.linkedinUrl,
    headline: linkedin.headline,
    avatarUrl,
    companyDomain,
    companyLinkedinUrl: companyDetails.linkedinUrl,
    companyEmployees: companyDetails.employees,
    companyAddress: companyDetails.address,
    companyEnriched: companyDetails.enriched as EnrichResult["companyEnriched"],
    summary,
    enriched,
  };
  return NextResponse.json(result);
}
