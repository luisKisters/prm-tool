import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { CommitSchema, type CommitResult } from "@/lib/types";
import { upsertPerson, personUrl } from "@/lib/twenty";
import { upsertCompany } from "@/lib/twentyCompany";
import { createContact } from "@/lib/google";

export const runtime = "nodejs";
export const maxDuration = 60;

/**
 * Persists a confirmed contact: upserts the Twenty person (idempotent on prmId) then creates the
 * Google contact carrying the stable prmId as a userDefined field.
 */
export async function POST(req: Request) {
  const unauthorized = requireAuth(req);
  if (unauthorized) return unauthorized;

  let raw: unknown;
  try {
    raw = await req.json();
  } catch {
    return NextResponse.json({ error: "Expected application/json" }, { status: 400 });
  }

  const parsed = CommitSchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json(
      { error: "Invalid body", details: parsed.error.flatten() },
      { status: 400 },
    );
  }
  const body = parsed.data;

  // Upsert + link the company first (when we have a domain) so the person carries the relation.
  const twentyCompanyId = body.companyDomain
    ? await upsertCompany({
        name: body.company || body.companyDomain,
        domain: body.companyDomain,
        linkedinUrl: body.companyLinkedinUrl,
        employees: body.companyEmployees,
        address: body.companyAddress,
        enriched: body.companyEnriched,
      })
    : null;

  const twentyId = await upsertPerson(body, twentyCompanyId);
  const twentyUrl = twentyId ? personUrl(twentyId) : null;
  const googleResourceName = await createContact(body, twentyUrl);

  const result: CommitResult = {
    prmId: body.prmId,
    twentyId,
    twentyCompanyId,
    twentyUrl,
    googleResourceName,
  };
  return NextResponse.json(result);
}
