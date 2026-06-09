import { describe, it, expect } from "vitest";
import { buildCompanyBody } from "../twentyCompany";
import { parseEmployees, toDomain } from "../serper";
import { extractTitle } from "../html";
import { splitPhone } from "../phone";

describe("buildCompanyBody", () => {
  it("builds the LINKS domainName composite", () => {
    const body = buildCompanyBody({ name: "Acme", domain: "acme.com" });
    expect(body.domainName).toEqual({
      primaryLinkUrl: "acme.com",
      primaryLinkLabel: "",
      secondaryLinks: [],
    });
  });

  it("includes employees, linkedin, address and enriched when present", () => {
    const body = buildCompanyBody({
      name: "Acme",
      domain: "acme.com",
      linkedinUrl: "https://linkedin.com/company/acme",
      employees: 42,
      address: "Berlin",
      enriched: ["DOMAIN", "EMPLOYEES"],
    });
    expect(body.employees).toBe(42);
    expect(body.linkedinLink).toEqual({
      primaryLinkUrl: "https://linkedin.com/company/acme",
      primaryLinkLabel: "LinkedIn",
    });
    expect(body.address).toEqual({ addressCity: "Berlin" });
    expect(body.enriched).toEqual(["DOMAIN", "EMPLOYEES"]);
  });

  it("omits optional fields when absent", () => {
    const body = buildCompanyBody({ name: "Acme", domain: "" });
    expect(body.domainName).toBeUndefined();
    expect(body.employees).toBeUndefined();
    expect(body.address).toBeUndefined();
    expect(body.enriched).toBeUndefined();
  });
});

describe("parseEmployees", () => {
  it("extracts a number from a knowledge-graph attribute", () => {
    expect(parseEmployees("10,001+ employees")).toBe(10001);
    expect(parseEmployees("~250")).toBe(250);
  });
  it("returns null when there is no number", () => {
    expect(parseEmployees(undefined)).toBeNull();
    expect(parseEmployees("unknown")).toBeNull();
  });
});

describe("toDomain", () => {
  it("strips scheme, www and path", () => {
    expect(toDomain("https://www.acme.com/about")).toBe("acme.com");
    expect(toDomain("acme.com")).toBe("acme.com");
  });
  it("returns empty for junk", () => {
    expect(toDomain("not a url")).toBe("");
  });
});

describe("extractTitle", () => {
  it("pulls the <title> and decodes entities", () => {
    expect(extractTitle("<html><head><title>Acme &amp; Co</title></head>")).toBe("Acme & Co");
  });
  it("tolerates attributes and whitespace", () => {
    expect(extractTitle('<title data-x="y">\n  Hello \n</title>')).toBe("Hello");
  });
  it("returns empty when no title", () => {
    expect(extractTitle("<html><body>hi</body></html>")).toBe("");
  });
});

describe("splitPhone", () => {
  it("splits an international number into Twenty parts", () => {
    expect(splitPhone("+49 151 23456789")).toEqual({
      primaryPhoneNumber: "15123456789",
      primaryPhoneCallingCode: "+49",
      primaryPhoneCountryCode: "DE",
    });
  });
  it("returns null for empty input", () => {
    expect(splitPhone("")).toBeNull();
  });
});
