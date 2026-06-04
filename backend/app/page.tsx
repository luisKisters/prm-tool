export default function Home() {
  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: 32, maxWidth: 640 }}>
      <h1>PRM Tool API</h1>
      <p>
        Stateless backend for the PRM Tool Android app. It exposes two endpoints:
      </p>
      <ul>
        <li>
          <code>POST /api/enrich</code> — transcribe voice, find LinkedIn, avatar, company
          domain, summarize. No external writes.
        </li>
        <li>
          <code>POST /api/commit</code> — create the Twenty person and Google contact after the
          user confirms.
        </li>
      </ul>
      <p>
        Health: <code>GET /api/health</code>
      </p>
    </main>
  );
}
