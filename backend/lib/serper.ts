/** Serper.dev helpers: LinkedIn lookup, best-effort avatar, and company domain. */

interface OrganicResult {
  link?: string;
  snippet?: string;
  title?: string;
  imageUrl?: string;
}
interface SearchResponse {
  organic?: OrganicResult[];
  knowledgeGraph?: { website?: string; descriptionLink?: string };
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

function toDomain(url: string): string {
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
