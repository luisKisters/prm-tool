import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

// The route handler orchestrates four upstream calls. We mock each lib so the test asserts the
// *orchestration* (concurrency + dependency wiring), not the upstream HTTP behavior.
vi.mock("@/lib/auth", () => ({ requireAuth: () => null }));

const transcribe = vi.fn();
const findLinkedIn = vi.fn();
const findAvatar = vi.fn();
const findCompanyDomain = vi.fn();
const findCompanyDetails = vi.fn();
const summarizeNote = vi.fn();

vi.mock("@/lib/groq", () => ({ transcribe: (...a: unknown[]) => transcribe(...a) }));
vi.mock("@/lib/serper", () => ({
  findLinkedIn: (...a: unknown[]) => findLinkedIn(...a),
  findAvatar: (...a: unknown[]) => findAvatar(...a),
  findCompanyDomain: (...a: unknown[]) => findCompanyDomain(...a),
  findCompanyDetails: (...a: unknown[]) => findCompanyDetails(...a),
}));
vi.mock("@/lib/openrouter", () => ({ summarizeNote: (...a: unknown[]) => summarizeNote(...a) }));

import { POST } from "@/app/api/enrich/route";

function buildRequest(fields: Record<string, string>, voice?: File): Request {
  const form = new FormData();
  for (const [k, v] of Object.entries(fields)) form.append(k, v);
  if (voice) form.append("voice", voice);
  return new Request("http://localhost/api/enrich", { method: "POST", body: form });
}

beforeEach(() => {
  findLinkedIn.mockResolvedValue({ linkedinUrl: "https://linkedin.com/in/jane", headline: "CEO" });
  findAvatar.mockResolvedValue("https://img/jane.jpg");
  findCompanyDomain.mockResolvedValue("acme.com");
  findCompanyDetails.mockResolvedValue({
    linkedinUrl: "https://linkedin.com/company/acme",
    employees: 42,
    address: "Berlin",
    enriched: ["LINKEDIN", "EMPLOYEES", "ADDRESS"],
  });
  summarizeNote.mockResolvedValue("A short summary.");
  transcribe.mockResolvedValue("transcribed words");
});

afterEach(() => vi.clearAllMocks());

describe("POST /api/enrich", () => {
  it("returns 400 when clientId is missing", async () => {
    const res = await POST(buildRequest({ firstName: "Jane" }));
    expect(res.status).toBe(400);
  });

  it("assembles the enriched result and tags from all upstream calls", async () => {
    const res = await POST(
      buildRequest({ clientId: "c1", firstName: "Jane", lastName: "Doe", company: "Acme", note: "met at conf" }),
    );
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body).toMatchObject({
      prmId: "c1",
      linkedinUrl: "https://linkedin.com/in/jane",
      headline: "CEO",
      avatarUrl: "https://img/jane.jpg",
      companyDomain: "acme.com",
      companyLinkedinUrl: "https://linkedin.com/company/acme",
      companyEmployees: 42,
      companyAddress: "Berlin",
      summary: "A short summary.",
    });
    expect(body.enriched).toEqual(
      expect.arrayContaining(["LINKEDIN", "JOB_TITLE", "AVATAR", "COMPANY"]),
    );
    // findCompanyDetails ran concurrently with domain lookup (called with "" domain), so the
    // route folds the DOMAIN tag in afterwards once the resolved domain is known.
    expect(findCompanyDetails).toHaveBeenCalledWith("Acme", "");
    expect(body.companyEnriched).toEqual(["DOMAIN", "LINKEDIN", "EMPLOYEES", "ADDRESS"]);
  });

  it("fires the LinkedIn/avatar/company lookups WITHOUT waiting for transcription", async () => {
    // Transcription is gated on a manual release; the lookups must already have been invoked
    // before it resolves. This is the core regression guard for the parallelization fix.
    let releaseTranscript!: (v: string) => void;
    transcribe.mockReturnValue(
      new Promise<string>((resolve) => {
        releaseTranscript = resolve;
      }),
    );

    const voice = new File([new Uint8Array([1, 2, 3])], "voice.m4a", { type: "audio/m4a" });
    const resPromise = POST(
      buildRequest({ clientId: "c1", firstName: "Jane", lastName: "Doe", company: "Acme" }, voice),
    );

    // Let formData parsing + the Promise.all kickoff run, while transcription stays pending.
    await new Promise((r) => setTimeout(r, 20));

    expect(findLinkedIn).toHaveBeenCalledTimes(1);
    expect(findAvatar).toHaveBeenCalledTimes(1);
    expect(findCompanyDomain).toHaveBeenCalledTimes(1);
    // Summary must NOT have started yet — it depends on the transcript.
    expect(summarizeNote).not.toHaveBeenCalled();

    releaseTranscript("transcribed words");
    const res = await resPromise;
    expect(res.status).toBe(200);
    // Once transcription resolves, the summary runs with the transcript folded in.
    expect(summarizeNote).toHaveBeenCalledTimes(1);
    expect(String(summarizeNote.mock.calls[0][0])).toContain("transcribed words");
  });

  it("skips name-based lookups when no name is provided", async () => {
    const res = await POST(buildRequest({ clientId: "c1", company: "Acme" }));
    expect(res.status).toBe(200);
    expect(findLinkedIn).not.toHaveBeenCalled();
    expect(findAvatar).not.toHaveBeenCalled();
    // Company lookup still runs.
    expect(findCompanyDomain).toHaveBeenCalledTimes(1);
  });
});
