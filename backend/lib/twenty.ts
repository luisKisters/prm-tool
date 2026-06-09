/** Twenty CRM REST client. Creates/updates a Person, keyed by the stable `prmId` custom field. */
import { splitPhone } from "./phone";
import type { CommitBody } from "./types";

function baseUrl(): string {
  return (process.env.TWENTY_BASE_URL || "").replace(/\/$/, "");
}

function headers(): Record<string, string> {
  return {
    Authorization: `Bearer ${process.env.TWENTY_API_KEY || ""}`,
    "Content-Type": "application/json",
  };
}

export function personUrl(id: string): string {
  return `${baseUrl()}/object/person/${id}`;
}

/** Option values defined on the Person `source` MULTI_SELECT field (see scripts/setup-twenty-schema.mjs). */
export const SOURCE_OPTION_VALUES = ["YFN", "EVENT", "REFERRAL", "COLD_OUTREACH", "ONLINE"] as const;

/** Normalise a free-text source name to a Twenty select-option value (uppercase, underscores). */
export function normSourceValue(label: string): string {
  return label.trim().toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_+|_+$/g, "");
}

/**
 * Map the app's comma-separated `sources` string to the known Twenty `source` option values.
 * Unknown sources are dropped (Twenty rejects option values that aren't defined on the field).
 */
export function mapSourcesToOptions(sources: string): string[] {
  const known = new Set<string>(SOURCE_OPTION_VALUES);
  const out: string[] = [];
  for (const part of sources.split(",")) {
    const value = normSourceValue(part);
    if (value && known.has(value) && !out.includes(value)) out.push(value);
  }
  return out;
}

/** Find an existing person by the stable prmId custom field. Returns its id, or null. */
async function findByPrmId(prmId: string): Promise<string | null> {
  try {
    const url = `${baseUrl()}/rest/people?filter=prmId[eq]:${encodeURIComponent(prmId)}&limit=1`;
    const res = await fetch(url, { headers: headers() });
    if (!res.ok) return null;
    const json = (await res.json()) as { data?: { people?: { id?: string }[] } };
    return json.data?.people?.[0]?.id ?? null;
  } catch {
    return null;
  }
}

export function buildBody(body: CommitBody, companyId: string | null) {
  const phones = splitPhone(body.number);
  const link = body.linkedinUrl
    ? { primaryLinkUrl: body.linkedinUrl, primaryLinkLabel: "LinkedIn" }
    : null;
  const sources = mapSourcesToOptions(body.sources);
  // Manual email counts as a filled field; surface it in the Enriched multi-select too.
  const enriched = body.email && !body.enriched.includes("EMAIL")
    ? [...body.enriched, "EMAIL"]
    : body.enriched;

  return {
    prmId: body.prmId,
    name: { firstName: body.firstName, lastName: body.lastName },
    ...(body.email ? { emails: { primaryEmail: body.email, additionalEmails: [] } } : {}),
    ...(phones ? { phones } : {}),
    ...(body.headline ? { jobTitle: body.headline } : {}),
    ...(link ? { linkedinLink: link } : {}),
    ...(body.avatarUrl ? { avatarUrl: body.avatarUrl } : {}),
    ...(companyId ? { companyId } : {}),
    ...(sources.length ? { source: sources } : {}),
    ...(body.sourceDetails ? { sourceDetails: body.sourceDetails } : {}),
    enriched,
  };
}

/**
 * Idempotently upsert a Twenty person. If one already exists with this prmId we PATCH it,
 * otherwise we POST a new one. Links to a company when `companyId` is provided.
 * Returns the person id, or null on failure.
 */
export async function upsertPerson(
  body: CommitBody,
  companyId: string | null = null,
): Promise<string | null> {
  if (!process.env.TWENTY_API_KEY || !baseUrl()) return null;
  const payload = buildBody(body, companyId);

  const existingId = await findByPrmId(body.prmId);
  try {
    const res = existingId
      ? await fetch(`${baseUrl()}/rest/people/${existingId}`, {
          method: "PATCH",
          headers: headers(),
          body: JSON.stringify(payload),
        })
      : await fetch(`${baseUrl()}/rest/people`, {
          method: "POST",
          headers: headers(),
          body: JSON.stringify(payload),
        });

    if (!res.ok) return existingId;
    const json = (await res.json()) as {
      data?: { createPerson?: { id?: string }; updatePerson?: { id?: string } };
    };
    return json.data?.createPerson?.id ?? json.data?.updatePerson?.id ?? existingId;
  } catch {
    return existingId;
  }
}
