/** Transcribe a voice memo with Groq Whisper. Best-effort: returns "" on any failure. */
export async function transcribe(voice: File): Promise<string> {
  const key = process.env.GROQ_API_KEY;
  if (!key) return "";

  const form = new FormData();
  form.append("file", voice, voice.name || "voice.m4a");
  form.append("model", "whisper-large-v3");
  form.append("response_format", "json");

  try {
    const res = await fetch("https://api.groq.com/openai/v1/audio/transcriptions", {
      method: "POST",
      headers: { Authorization: `Bearer ${key}` },
      body: form,
    });
    if (!res.ok) return "";
    const json = (await res.json()) as { text?: string };
    return (json.text || "").trim();
  } catch {
    return "";
  }
}
