import { NextResponse } from "next/server";
import { exchangeCodeForRefreshToken } from "@/lib/google";
import { googleCallbackUrl, originOf } from "@/lib/origin";
import { verifyState } from "@/lib/oauthState";

export const runtime = "nodejs";

/**
 * Google redirects here after consent. Verifies the signed state (proves the initiator knew the
 * app secret), exchanges the code, and shows the refresh token to paste into GOOGLE_REFRESH_TOKEN.
 */
export async function GET(req: Request) {
  const secret = process.env.APP_API_SECRET || "";
  const origin = originOf(req);
  const params = new URL(req.url).searchParams;

  if (params.get("error") || !params.get("code")) {
    return NextResponse.redirect(`${origin}/setup?error=server`, { status: 303 });
  }
  if (!verifyState(params.get("state"), secret)) {
    return NextResponse.redirect(`${origin}/setup?error=state`, { status: 303 });
  }

  let refreshToken: string | null = null;
  try {
    refreshToken = await exchangeCodeForRefreshToken(googleCallbackUrl(req), params.get("code")!);
  } catch {
    return NextResponse.redirect(`${origin}/setup?error=server`, { status: 303 });
  }
  if (!refreshToken) {
    return NextResponse.redirect(`${origin}/setup?error=notoken`, { status: 303 });
  }

  return new NextResponse(tokenPage(refreshToken), {
    status: 200,
    headers: { "Content-Type": "text/html; charset=utf-8" },
  });
}

function tokenPage(token: string): string {
  // token is a Google-issued opaque string; escape just in case before embedding.
  const safe = token.replace(/[&<>"]/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!,
  );
  return `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>PRM Tool · Refresh token</title></head>
<body style="font-family:system-ui,sans-serif;padding:32px;max-width:640px">
<h1>✅ Google connected</h1>
<p>Set this as <code>GOOGLE_REFRESH_TOKEN</code> in your Vercel project's environment variables,
then redeploy. Keep it secret.</p>
<pre style="white-space:pre-wrap;word-break:break-all;background:#f3edf7;padding:16px;border-radius:12px">${safe}</pre>
<p style="color:#555">Once it's set and redeployed, confirming a contact in the app will create the Google contact too.</p>
</body></html>`;
}
