import { fetchWithTimeout } from "@/lib/http";

/** Summarize a contact note with Kimi K2.6 via OpenRouter. Best-effort: "" on failure. */
const DEFAULT_MODEL = "moonshotai/kimi-k2.6";

export async function summarizeNote(fullNote: string): Promise<string> {
  const key = process.env.OPENROUTER_API_KEY;
  const note = (fullNote || "").trim();
  if (!key || !note) return "";

  const model = process.env.OPENROUTER_MODEL || DEFAULT_MODEL;
  try {
    const res = await fetchWithTimeout("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${key}`,
        "Content-Type": "application/json",
      },
      timeoutMs: 20_000,
      body: JSON.stringify({
        model,
        temperature: 0.3,
        messages: [
          {
            role: "user",
            content:
              "Summarize the following contact note in 1-2 concise sentences for CRM context. " +
              "Output ONLY the summary, no preamble.\n\n" +
              note,
          },
        ],
      }),
    });
    if (!res.ok) return "";
    const json = (await res.json()) as {
      choices?: { message?: { content?: string } }[];
    };
    return (json.choices?.[0]?.message?.content || "").trim();
  } catch {
    return "";
  }
}
