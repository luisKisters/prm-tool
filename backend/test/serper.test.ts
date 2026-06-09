import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  findAvatar,
  findCompanyDomain,
  findLinkedIn,
  lookupLinkedInProfile,
} from "@/lib/serper";

const realFetch = globalThis.fetch;
const OLD_KEY = process.env.SERPER_API_KEY;

/** Stub fetch with a single JSON payload, capturing the request body for assertions. */
function mockSerper(payload: unknown) {
  const calls: { url: string; body: unknown }[] = [];
  globalThis.fetch = vi.fn(async (url: string, init?: RequestInit) => {
    calls.push({ url, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return new Response(JSON.stringify(payload), { status: 200 });
  }) as unknown as typeof fetch;
  return calls;
}

beforeEach(() => {
  process.env.SERPER_API_KEY = "test-key";
});

afterEach(() => {
  globalThis.fetch = realFetch;
  process.env.SERPER_API_KEY = OLD_KEY;
  vi.restoreAllMocks();
});

describe("findLinkedIn", () => {
  it("returns the first organic /in/ result with its snippet as headline", async () => {
    mockSerper({
      organic: [
        { link: "https://example.com/not-linkedin", snippet: "nope" },
        { link: "https://www.linkedin.com/in/jane-doe", snippet: "CEO at Acme" },
      ],
    });
    const res = await findLinkedIn("Jane Doe", "Acme");
    expect(res.linkedinUrl).toBe("https://www.linkedin.com/in/jane-doe");
    expect(res.headline).toBe("CEO at Acme");
  });

  it("returns empty fields when no /in/ profile is present", async () => {
    mockSerper({ organic: [{ link: "https://acme.com", snippet: "homepage" }] });
    const res = await findLinkedIn("Jane Doe", "Acme");
    expect(res).toEqual({ linkedinUrl: "", headline: "" });
  });

  it("returns empty fields and skips the call when no API key is set", async () => {
    delete process.env.SERPER_API_KEY;
    const calls = mockSerper({ organic: [] });
    const res = await findLinkedIn("Jane Doe", "Acme");
    expect(res).toEqual({ linkedinUrl: "", headline: "" });
    expect(calls).toHaveLength(0);
  });
});

describe("findCompanyDomain", () => {
  it("prefers the knowledge-graph website and strips www.", async () => {
    mockSerper({ knowledgeGraph: { website: "https://www.acme.com/about" } });
    expect(await findCompanyDomain("Acme")).toBe("acme.com");
  });

  it("skips aggregator domains like linkedin.com and falls through to a real site", async () => {
    mockSerper({
      organic: [
        { link: "https://www.linkedin.com/company/acme" },
        { link: "https://en.wikipedia.org/wiki/Acme" },
        { link: "https://acme.io/products" },
      ],
    });
    expect(await findCompanyDomain("Acme")).toBe("acme.io");
  });

  it("returns empty string for blank company without calling the API", async () => {
    const calls = mockSerper({});
    expect(await findCompanyDomain("   ")).toBe("");
    expect(calls).toHaveLength(0);
  });
});

describe("findAvatar", () => {
  it("returns the first image URL, falling back to the thumbnail", async () => {
    mockSerper({ images: [{ thumbnailUrl: "https://img/thumb.jpg" }] });
    expect(await findAvatar("Jane Doe", "Acme")).toBe("https://img/thumb.jpg");
  });

  it("returns empty string when there are no images", async () => {
    mockSerper({ images: [] });
    expect(await findAvatar("Jane Doe", "Acme")).toBe("");
  });
});

describe("lookupLinkedInProfile", () => {
  it("matches the result whose /in/ slug equals the URL's slug and parses the headline", async () => {
    mockSerper({
      organic: [
        { link: "https://www.linkedin.com/in/someone-else", title: "Someone Else - CTO | LinkedIn" },
        {
          link: "https://www.linkedin.com/in/jane-doe",
          title: "Jane Doe - CEO at Acme | LinkedIn",
          imageUrl: "https://img/jane.jpg",
        },
      ],
    });
    const res = await lookupLinkedInProfile("https://linkedin.com/in/jane-doe");
    expect(res.headline).toBe("CEO at Acme");
    expect(res.avatarUrl).toBe("https://img/jane.jpg");
  });

  it("returns empty result for a non-LinkedIn URL without calling the API", async () => {
    const calls = mockSerper({});
    const res = await lookupLinkedInProfile("https://example.com/jane");
    expect(res).toEqual({ headline: "", avatarUrl: "" });
    expect(calls).toHaveLength(0);
  });
});
