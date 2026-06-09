import { describe, it, expect } from "vitest";
import { normSourceValue, mapSourcesToOptions, buildBody } from "../twenty";
import { CommitSchema } from "../types";

describe("normSourceValue", () => {
  it("uppercases and underscores non-alphanumerics", () => {
    expect(normSourceValue("Cold outreach")).toBe("COLD_OUTREACH");
    expect(normSourceValue("  YFN ")).toBe("YFN");
    expect(normSourceValue("Online")).toBe("ONLINE");
  });
});

describe("mapSourcesToOptions", () => {
  it("maps known sources to option values, dropping unknown ones", () => {
    expect(mapSourcesToOptions("YFN, Referral")).toEqual(["YFN", "REFERRAL"]);
    expect(mapSourcesToOptions("Cold outreach")).toEqual(["COLD_OUTREACH"]);
    expect(mapSourcesToOptions("Twitter, Online")).toEqual(["ONLINE"]);
  });

  it("de-duplicates and tolerates blanks", () => {
    expect(mapSourcesToOptions("YFN, yfn, , YFN")).toEqual(["YFN"]);
    expect(mapSourcesToOptions("")).toEqual([]);
  });
});

describe("buildBody", () => {
  const base = CommitSchema.parse({ prmId: "p1", firstName: "Ada", lastName: "Lovelace" });

  it("includes the emails composite and EMAIL tag when an email is present", () => {
    const body = buildBody({ ...base, email: "ada@acme.com" }, null);
    expect(body.emails).toEqual({ primaryEmail: "ada@acme.com", additionalEmails: [] });
    expect(body.enriched).toContain("EMAIL");
  });

  it("omits emails and does not add EMAIL when absent", () => {
    const body = buildBody(base, null);
    expect(body.emails).toBeUndefined();
    expect(body.enriched).not.toContain("EMAIL");
  });

  it("does not duplicate an existing EMAIL tag", () => {
    const body = buildBody({ ...base, email: "ada@acme.com", enriched: ["EMAIL"] }, null);
    expect(body.enriched.filter((t) => t === "EMAIL")).toHaveLength(1);
  });

  it("links the company when a companyId is provided", () => {
    expect(buildBody(base, "c123").companyId).toBe("c123");
    expect(buildBody(base, null).companyId).toBeUndefined();
  });

  it("maps sources and source details onto the person", () => {
    const body = buildBody({ ...base, sources: "YFN, Online", sourceDetails: "met at a meetup" }, null);
    expect(body.source).toEqual(["YFN", "ONLINE"]);
    expect(body.sourceDetails).toBe("met at a meetup");
  });

  it("omits the source field when no known sources match", () => {
    expect(buildBody({ ...base, sources: "Twitter" }, null).source).toBeUndefined();
  });
});
