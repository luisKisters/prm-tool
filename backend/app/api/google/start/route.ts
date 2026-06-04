import { NextResponse } from "next/server";
import { buildConsentUrl } from "@/lib/google";
import { googleCallbackUrl, originOf } from "@/lib/origin";
import { makeState } from "@/lib/oauthState";

export const runtime = "nodejs";

/** Verifies the app secret, then redirects to Google's consent screen with a signed state. */
export async function POST(req: Request) {
  const secret = process.env.APP_API_SECRET;
  const origin = originOf(req);

  if (!secret) {
    return NextResponse.redirect(`${origin}/setup?error=server`, { status: 303 });
  }

  let submitted = "";
  try {
    const form = await req.formData();
    submitted = form.get("secret")?.toString() ?? "";
  } catch {
    return NextResponse.redirect(`${origin}/setup?error=server`, { status: 303 });
  }

  if (submitted !== secret) {
    return NextResponse.redirect(`${origin}/setup?error=secret`, { status: 303 });
  }

  const url = buildConsentUrl(googleCallbackUrl(req), makeState(secret));
  if (!url) {
    return NextResponse.redirect(`${origin}/setup?error=server`, { status: 303 });
  }
  return NextResponse.redirect(url, { status: 303 });
}
