/** Twenty CRM REST client for the Company object. Upserts a company keyed by its domain. */

function baseUrl(): string {
  return (process.env.TWENTY_BASE_URL || "").replace(/\/$/, "");
}

function headers(): Record<string, string> {
  return {
    Authorization: `Bearer ${process.env.TWENTY_API_KEY || ""}`,
    "Content-Type": "application/json",
  };
}

export interface CompanyUpsert {
  name: string;
  domain: string;
  linkedinUrl?: string;
  employees?: number | null;
  address?: string;
  enriched?: string[];
}

/** Find an existing company by domain (then by name). Returns its id, or null. */
async function findCompany(domain: string, name: string): Promise<string | null> {
  const tryFilter = async (filter: string): Promise<string | null> => {
    try {
      const url = `${baseUrl()}/rest/companies?filter=${filter}&limit=1`;
      const res = await fetch(url, { headers: headers() });
      if (!res.ok) return null;
      const json = (await res.json()) as { data?: { companies?: { id?: string }[] } };
      return json.data?.companies?.[0]?.id ?? null;
    } catch {
      return null;
    }
  };
  if (domain) {
    const byDomain = await tryFilter(
      `domainName.primaryLinkUrl[eq]:${encodeURIComponent(domain)}`,
    );
    if (byDomain) return byDomain;
  }
  if (name) return tryFilter(`name[eq]:${encodeURIComponent(name)}`);
  return null;
}

function buildCompanyBody(c: CompanyUpsert) {
  return {
    name: c.name,
    ...(c.domain
      ? { domainName: { primaryLinkUrl: c.domain, primaryLinkLabel: "", secondaryLinks: [] } }
      : {}),
    ...(c.linkedinUrl
      ? { linkedinLink: { primaryLinkUrl: c.linkedinUrl, primaryLinkLabel: "LinkedIn" } }
      : {}),
    ...(typeof c.employees === "number" ? { employees: c.employees } : {}),
    ...(c.address ? { address: { addressCity: c.address } } : {}),
    ...(c.enriched && c.enriched.length ? { enriched: c.enriched } : {}),
  };
}

/**
 * Idempotently upsert a Twenty company by domain. PATCHes an existing match, otherwise POSTs a new
 * one. Returns the company id, or null on failure / when no domain or name is provided.
 */
export async function upsertCompany(c: CompanyUpsert): Promise<string | null> {
  if (!process.env.TWENTY_API_KEY || !baseUrl()) return null;
  if (!c.domain && !c.name) return null;
  const payload = buildCompanyBody(c);

  const existingId = await findCompany(c.domain, c.name);
  try {
    const res = existingId
      ? await fetch(`${baseUrl()}/rest/companies/${existingId}`, {
          method: "PATCH",
          headers: headers(),
          body: JSON.stringify(payload),
        })
      : await fetch(`${baseUrl()}/rest/companies`, {
          method: "POST",
          headers: headers(),
          body: JSON.stringify(payload),
        });
    if (!res.ok) return existingId;
    const json = (await res.json()) as {
      data?: { createCompany?: { id?: string }; updateCompany?: { id?: string } };
    };
    return json.data?.createCompany?.id ?? json.data?.updateCompany?.id ?? existingId;
  } catch {
    return existingId;
  }
}

export { buildCompanyBody };
