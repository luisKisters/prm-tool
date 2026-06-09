/**
 * Deferred: best-effort work-email discovery via Gemini with Google Search grounding.
 *
 * Not implemented yet — the app collects email manually for now. When enabled, this will take a
 * name + company domain, ask Gemini (grounded) for the most likely work email, and return it so
 * /api/enrich can pre-fill the field and set the EMAIL enriched tag. Reads GEMINI_API_KEY.
 */
export async function findEmailWithGemini(fullName: string, companyDomain: string): Promise<string> {
  // TODO: implement grounded lookup. Intentionally a no-op until GEMINI_API_KEY wiring lands.
  void fullName;
  void companyDomain;
  return "";
}
