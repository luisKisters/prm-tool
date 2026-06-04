import { NextResponse } from "next/server";
import { requireAuth } from "@/lib/auth";
import { CommitSchema, type CommitResult } from "@/lib/types";
import { upsertPerson, personUrl } from "@/lib/twenty";
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

  const twentyId = await upsertPerson(body);
  const twentyUrl = twentyId ? personUrl(twentyId) : null;
  const googleResourceName = await createContact(body, twentyUrl);

  const result: CommitResult = {
    prmId: body.prmId,
    twentyId,
    twentyUrl,
    googleResourceName,
  };
  return NextResponse.json(result);
}
