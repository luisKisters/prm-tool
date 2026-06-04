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

function buildBody(body: CommitBody) {
  const phones = splitPhone(body.number);
  const link = body.linkedinUrl
    ? { primaryLinkUrl: body.linkedinUrl, primaryLinkLabel: "LinkedIn" }
    : null;

  return {
    prmId: body.prmId,
    name: { firstName: body.firstName, lastName: body.lastName },
    ...(phones ? { phones } : {}),
    ...(body.headline ? { jobTitle: body.headline } : {}),
    ...(link ? { linkedinLink: link } : {}),
    ...(body.avatarUrl ? { avatarUrl: body.avatarUrl } : {}),
    enriched: body.enriched,
  };
}

/**
 * Idempotently upsert a Twenty person. If one already exists with this prmId we PATCH it,
 * otherwise we POST a new one. Returns the person id, or null on failure.
 */
export async function upsertPerson(body: CommitBody): Promise<string | null> {
  if (!process.env.TWENTY_API_KEY || !baseUrl()) return null;
  const payload = buildBody(body);

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
