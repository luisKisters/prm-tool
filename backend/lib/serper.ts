/** Serper.dev helpers: LinkedIn lookup, best-effort avatar, and company domain. */

interface OrganicResult {
  link?: string;
  snippet?: string;
  title?: string;
  imageUrl?: string;
}
interface SearchResponse {
  organic?: OrganicResult[];
  knowledgeGraph?: {
    website?: string;
    descriptionLink?: string;
    title?: string;
    type?: string;
    attributes?: Record<string, string>;
  };
}
interface ImagesResponse {
  images?: { imageUrl?: string; thumbnailUrl?: string }[];
}

async function serper<T>(path: string, body: Record<string, unknown>): Promise<T | null> {
  const key = process.env.SERPER_API_KEY;
  if (!key) return null;
  try {
    const res = await fetch(`https://google.serper.dev${path}`, {
      method: "POST",
      headers: { "X-API-KEY": key, "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    return null;
  }
}

/** Find the most likely LinkedIn profile URL + headline snippet for a person. */
export async function findLinkedIn(
  fullName: string,
  company: string,
): Promise<{ linkedinUrl: string; headline: string }> {
  const q = `"${fullName}" ${company} site:linkedin.com/in`.trim();
  const data = await serper<SearchResponse>("/search", { q, num: 3, gl: "us", hl: "en" });
  const hit = data?.organic?.find((o) => o.link?.includes("linkedin.com/in"));
  return {
    linkedinUrl: hit?.link ?? "",
    headline: hit?.snippet ?? "",
  };
}

/**
 * Best-effort lookup of a *specific* LinkedIn profile's headline + avatar from its URL.
 * Searches for the exact profile; pulls the headline from the result title (falling back to the
 * snippet) and the photo from the result's thumbnail (falling back to an image search by name).
 * Returns empty strings when nothing is found — LinkedIn has no public API, so this is approximate.
 */
export async function lookupLinkedInProfile(
  url: string,
  name = "",
): Promise<{ headline: string; avatarUrl: string }> {
  const slug = linkedinSlug(url);
  if (!slug) return { headline: "", avatarUrl: "" };

  const data = await serper<SearchResponse>("/search", { q: url, num: 5, gl: "us", hl: "en" });
  const hit =
    data?.organic?.find((o) => linkedinSlug(o.link ?? "") === slug) ??
    data?.organic?.find((o) => (o.link ?? "").includes("linkedin.com/in"));

  const headline = headlineFromTitle(hit?.title) || (hit?.snippet ?? "").trim();
  let avatarUrl = (hit?.imageUrl ?? "").trim();
  if (!avatarUrl) {
    const query = name.trim() || nameFromTitle(hit?.title) || slug.replace(/[-_]+/g, " ");
    if (query) avatarUrl = await findAvatar(query, "");
  }
  return { headline, avatarUrl };
}

/** Extract the lowercased `/in/<slug>` handle from a LinkedIn URL, or "" if it isn't one. */
function linkedinSlug(url: string): string {
  const trimmed = url.trim();
  if (!trimmed) return "";
  try {
    const u = new URL(trimmed.startsWith("http") ? trimmed : `https://${trimmed}`);
    if (!u.hostname.toLowerCase().includes("linkedin.com")) return "";
    const m = u.pathname.match(/\/in\/([^/?#]+)/i);
    return m ? decodeURIComponent(m[1]).toLowerCase() : "";
  } catch {
    return "";
  }
}

/** Drop a trailing "| LinkedIn" / "- LinkedIn" from a search-result title. */
function stripLinkedInSuffix(s: string): string {
  return s.replace(/\s*[|\-–—]\s*LinkedIn.*$/i, "").trim();
}

/** Google titles look like "Name - Headline | LinkedIn"; return the headline part. */
function headlineFromTitle(title?: string): string {
  if (!title) return "";
  const cleaned = stripLinkedInSuffix(title);
  const dash = cleaned.indexOf(" - ");
  return dash >= 0 ? cleaned.slice(dash + 3).trim() : "";
}

/** Google titles look like "Name - Headline | LinkedIn"; return the name part. */
function nameFromTitle(title?: string): string {
  if (!title) return "";
  const cleaned = stripLinkedInSuffix(title);
  const dash = cleaned.indexOf(" - ");
  return (dash > 0 ? cleaned.slice(0, dash) : cleaned).trim();
}

/** Best-effort profile picture from image search. Frequently empty / approximate. */
export async function findAvatar(fullName: string, company: string): Promise<string> {
  const q = `${fullName} ${company} linkedin`.trim();
  const data = await serper<ImagesResponse>("/images", { q, num: 1, gl: "us", hl: "en" });
  const first = data?.images?.[0];
  return first?.imageUrl || first?.thumbnailUrl || "";
}

/** Resolve a company name to a bare domain (e.g. "acme.com"). */
export async function findCompanyDomain(company: string): Promise<string> {
  if (!company.trim()) return "";
  const data = await serper<SearchResponse>("/search", { q: company, num: 3, gl: "us", hl: "en" });

  const candidates: string[] = [];
  if (data?.knowledgeGraph?.website) candidates.push(data.knowledgeGraph.website);
  for (const o of data?.organic ?? []) if (o.link) candidates.push(o.link);

  for (const url of candidates) {
    const domain = toDomain(url);
    if (domain && !isAggregator(domain)) return domain;
  }
  return "";
}

/** Parse an employee count from a knowledge-graph attribute value like "10,001+ employees". */
export function parseEmployees(value: string | undefined): number | null {
  if (!value) return null;
  const m = value.replace(/,/g, "").match(/\d+/);
  return m ? Number(m[0]) : null;
}

/** Pick the first knowledge-graph attribute whose key matches any of the given keywords. */
function attr(attrs: Record<string, string> | undefined, keywords: string[]): string {
  if (!attrs) return "";
  for (const [k, v] of Object.entries(attrs)) {
    const lk = k.toLowerCase();
    if (keywords.some((kw) => lk.includes(kw))) return v;
  }
  return "";
}

/**
 * Best-effort company enrichment: LinkedIn company page, employee count, and HQ location.
 * Returns the fields plus the set of company `enriched` tags for whatever was found.
 */
export async function findCompanyDetails(
  name: string,
  domain: string,
): Promise<{
  linkedinUrl: string;
  employees: number | null;
  address: string;
  enriched: string[];
}> {
  const enriched: string[] = [];
  if (domain) enriched.push("DOMAIN");
  if (!name.trim()) return { linkedinUrl: "", employees: null, address: "", enriched };

  const [li, kg] = await Promise.all([
    serper<SearchResponse>("/search", {
      q: `${name} site:linkedin.com/company`,
      num: 3,
      gl: "us",
      hl: "en",
    }),
    serper<SearchResponse>("/search", { q: name, num: 3, gl: "us", hl: "en" }),
  ]);

  const linkedinUrl =
    li?.organic?.find((o) => (o.link ?? "").includes("linkedin.com/company"))?.link ?? "";
  const attrs = kg?.knowledgeGraph?.attributes;
  const employees = parseEmployees(attr(attrs, ["employee"]));
  const address = attr(attrs, ["headquarter", "address", "location"]).trim();

  if (linkedinUrl) enriched.push("LINKEDIN");
  if (employees !== null) enriched.push("EMPLOYEES");
  if (address) enriched.push("ADDRESS");
  return { linkedinUrl, employees, address, enriched };
}

export function toDomain(url: string): string {
  try {
    const host = new URL(url.startsWith("http") ? url : `https://${url}`).hostname;
    return host.replace(/^www\./, "");
  } catch {
    return "";
  }
}

function isAggregator(domain: string): boolean {
  const blocked = [
    "linkedin.com", "wikipedia.org", "facebook.com", "twitter.com", "x.com",
    "instagram.com", "youtube.com", "crunchbase.com", "bloomberg.com",
    "glassdoor.com", "indeed.com", "google.com",
  ];
  return blocked.some((b) => domain === b || domain.endsWith("." + b));
}
