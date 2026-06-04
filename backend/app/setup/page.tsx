/**
 * Secret-gated setup page. Enter the app secret and connect a Google account to mint the
 * GOOGLE_REFRESH_TOKEN needed for writing Google Contacts. The secret is verified server-side
 * in /api/google/start before the Google consent screen is shown.
 */
export const dynamic = "force-dynamic";

export default async function SetupPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;
  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: 32, maxWidth: 560 }}>
      <h1>PRM Tool · Setup</h1>
      <p>Connect a Google account so the backend can write to Google Contacts.</p>

      {error ? (
        <p style={{ color: "#b3261e", fontWeight: 600 }}>
          {error === "secret"
            ? "Wrong app secret."
            : error === "state"
              ? "Link expired or invalid — start again."
              : error === "notoken"
                ? "Google did not return a refresh token. Revoke access at myaccount.google.com/permissions and retry."
                : "Something went wrong — try again."}
        </p>
      ) : null}

      <form
        method="POST"
        action="/api/google/start"
        style={{ display: "flex", flexDirection: "column", gap: 12, marginTop: 16 }}
      >
        <label style={{ fontWeight: 600 }}>App secret</label>
        <input
          type="password"
          name="secret"
          required
          autoComplete="off"
          style={{ padding: 10, fontSize: 16, borderRadius: 10, border: "1px solid #ccc" }}
        />
        <button
          type="submit"
          style={{
            padding: 12,
            fontSize: 16,
            fontWeight: 600,
            borderRadius: 10,
            border: "none",
            background: "#6750A4",
            color: "white",
            cursor: "pointer",
          }}
        >
          Connect Google Contacts
        </button>
      </form>
    </main>
  );
}
