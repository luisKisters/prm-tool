/** Google People API client. Creates a contact carrying the stable prmId as a userDefined field. */
import { google, people_v1 } from "googleapis";
import type { CommitBody } from "./types";

/** Scope needed to create/update Google contacts. */
export const GOOGLE_CONTACTS_SCOPE = "https://www.googleapis.com/auth/contacts";

function client(): people_v1.People | null {
  const { GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REFRESH_TOKEN } = process.env;
  if (!GOOGLE_CLIENT_ID || !GOOGLE_CLIENT_SECRET || !GOOGLE_REFRESH_TOKEN) return null;
  const auth = new google.auth.OAuth2(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET);
  auth.setCredentials({ refresh_token: GOOGLE_REFRESH_TOKEN });
  return google.people({ version: "v1", auth });
}

/** OAuth2 client for the interactive setup flow (no stored credentials yet). */
function oauthClient(redirectUri: string) {
  const { GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET } = process.env;
  if (!GOOGLE_CLIENT_ID || !GOOGLE_CLIENT_SECRET) return null;
  return new google.auth.OAuth2(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, redirectUri);
}

/** Build the Google consent URL. `prompt=consent` + offline access forces a refresh_token. */
export function buildConsentUrl(redirectUri: string, state: string): string | null {
  const auth = oauthClient(redirectUri);
  if (!auth) return null;
  return auth.generateAuthUrl({
    access_type: "offline",
    prompt: "consent",
    scope: [GOOGLE_CONTACTS_SCOPE],
    state,
  });
}

/** Exchange an authorization code for a refresh token. */
export async function exchangeCodeForRefreshToken(
  redirectUri: string,
  code: string,
): Promise<string | null> {
  const auth = oauthClient(redirectUri);
  if (!auth) return null;
  const { tokens } = await auth.getToken(code);
  return tokens.refresh_token ?? null;
}

function buildBiography(body: CommitBody, twentyUrl: string | null): string {
  const lines: string[] = [];
  if (body.summary) lines.push(body.summary, "", "---");
  if (body.linkedinUrl) lines.push(`LinkedIn: ${body.linkedinUrl}`);
  if (body.companyDomain) lines.push(`Company domain: ${body.companyDomain}`);
  if (body.events) lines.push(`Events: ${body.events}`);
  if (body.sources) lines.push(`Sources: ${body.sources}`);
  if (body.note) lines.push("", `Raw note: ${body.note}`);
  if (twentyUrl) lines.push(`Twenty: ${twentyUrl}`);
  return lines.join("\n");
}

/**
 * Create a Google contact. The prmId is stored as a userDefined field ("PRM ID") so the
 * contact stays identifiable after name/notes change. Best-effort photo from avatarUrl.
 * Returns the contact resourceName, or null on failure.
 */
export async function createContact(
  body: CommitBody,
  twentyUrl: string | null,
): Promise<string | null> {
  const people = client();
  if (!people) return null;

  const requestBody: people_v1.Schema$Person = {
    names: [{ givenName: body.firstName, familyName: body.lastName }],
    userDefined: [{ key: "PRM ID", value: body.prmId }],
    biographies: [{ value: buildBiography(body, twentyUrl), contentType: "TEXT_PLAIN" }],
  };

  if (body.company || body.headline) {
    requestBody.organizations = [
      { name: body.company || undefined, title: body.headline || undefined, current: true },
    ];
  }
  if (body.number) {
    requestBody.phoneNumbers = [{ value: body.number, type: "mobile" }];
  }
  if (body.linkedinUrl) {
    requestBody.urls = [{ value: body.linkedinUrl, type: "LinkedIn" }];
  }

  try {
    const res = await people.people.createContact({ requestBody });
    const resourceName = res.data.resourceName ?? null;
    if (resourceName && body.avatarUrl) {
      await setPhoto(people, resourceName, body.avatarUrl);
    }
    return resourceName;
  } catch {
    return null;
  }
}

async function setPhoto(
  people: people_v1.People,
  resourceName: string,
  avatarUrl: string,
): Promise<void> {
  try {
    const res = await fetch(avatarUrl);
    if (!res.ok) return;
    const buf = Buffer.from(await res.arrayBuffer());
    await people.people.updateContactPhoto({
      resourceName,
      requestBody: { photoBytes: buf.toString("base64") },
    });
  } catch {
    // best-effort; ignore photo failures
  }
}
