/**
 * One-time helper to mint a Google People API refresh token for the single PRM user.
 *
 * Usage:
 *   1. In Google Cloud Console create an OAuth 2.0 "Desktop app" client. Enable the People API.
 *   2. Export the creds, then run the script:
 *        GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... npm run google-auth
 *   3. Open the printed URL, approve, paste the code back.
 *   4. Copy the printed refresh_token into GOOGLE_REFRESH_TOKEN (Vercel env / .env.local).
 */
import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";
import { google } from "googleapis";

const clientId = process.env.GOOGLE_CLIENT_ID;
const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

if (!clientId || !clientSecret) {
  console.error("Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in the environment first.");
  process.exit(1);
}

// "Out of band" redirect works for Desktop-app clients without a running web server.
const oauth2 = new google.auth.OAuth2(clientId, clientSecret, "urn:ietf:wg:oauth:2.0:oob");

const url = oauth2.generateAuthUrl({
  access_type: "offline",
  prompt: "consent",
  scope: ["https://www.googleapis.com/auth/contacts"],
});

console.log("\n1) Open this URL and approve access:\n\n" + url + "\n");

const rl = createInterface({ input: stdin, output: stdout });
const code = (await rl.question("2) Paste the authorization code here: ")).trim();
rl.close();

const { tokens } = await oauth2.getToken(code);
if (!tokens.refresh_token) {
  console.error(
    "\nNo refresh_token returned. Revoke prior access at https://myaccount.google.com/permissions and retry.",
  );
  process.exit(1);
}
console.log("\nGOOGLE_REFRESH_TOKEN=" + tokens.refresh_token + "\n");
