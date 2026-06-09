// Idempotently create the PRM custom fields on Twenty CRM via the Metadata REST API.
//
//   node scripts/setup-twenty-schema.mjs
//
// Reads TWENTY_API_KEY / TWENTY_BASE_URL from the environment or backend/.env.local.
// Creates (skipping any that already exist):
//   - Person.source        MULTI_SELECT (YFN, Event, Referral, Cold outreach, Online)
//   - Person.sourceDetails TEXT
//   - Company.enriched     MULTI_SELECT (DOMAIN, LINKEDIN, EMPLOYEES, ADDRESS)
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));

function loadEnvLocal() {
  try {
    const text = readFileSync(join(__dirname, "..", ".env.local"), "utf8");
    for (const line of text.split("\n")) {
      const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
      if (m && !process.env[m[1]]) process.env[m[1]] = m[2].replace(/^["']|["']$/g, "");
    }
  } catch {
    /* no .env.local; rely on the environment */
  }
}
loadEnvLocal();

const BASE = (process.env.TWENTY_BASE_URL || "").replace(/\/$/, "");
const KEY = process.env.TWENTY_API_KEY || "";
if (!BASE || !KEY) {
  console.error("Missing TWENTY_BASE_URL / TWENTY_API_KEY");
  process.exit(1);
}

const PERSON_ID = "a38f26aa-f125-437d-83ee-7996980d3a0c";
const COMPANY_ID = "2f843427-9f42-4d45-b9a7-1c2cc66a8b76";
const COLORS = ["green", "blue", "sky", "orange", "yellow", "purple", "pink", "red", "turquoise"];

// Twenty select option `value`s must be alphanumeric/underscore (no spaces). Keep a readable label.
const normValue = (label) => label.trim().toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_+|_+$/g, "");
const opts = (labels) =>
  labels.map((label, position) => ({
    label,
    value: normValue(label),
    color: COLORS[position % COLORS.length],
    position,
  }));

const FIELDS = [
  {
    objectMetadataId: PERSON_ID,
    name: "source",
    label: "Source",
    type: "MULTI_SELECT",
    icon: "IconRoute",
    description: "How this contact was sourced (mirrors the PRM app source chips).",
    options: opts(["YFN", "Event", "Referral", "Cold outreach", "Online"]),
  },
  {
    objectMetadataId: PERSON_ID,
    name: "sourceDetails",
    label: "Source details",
    type: "TEXT",
    icon: "IconNotes",
    description: "Free-text detail about how/where this contact was sourced.",
  },
  {
    objectMetadataId: COMPANY_ID,
    name: "enriched",
    label: "Enriched",
    type: "MULTI_SELECT",
    icon: "IconSparkles",
    description: "Tracks which company fields have been enriched by the PRM Tool.",
    options: opts(["DOMAIN", "LINKEDIN", "EMPLOYEES", "ADDRESS"]),
  },
];

const headers = { Authorization: `Bearer ${KEY}`, "Content-Type": "application/json" };

async function existingFieldNames(objectId) {
  const res = await fetch(`${BASE}/rest/metadata/objects/${objectId}`, { headers });
  if (!res.ok) throw new Error(`fetch object ${objectId} failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  const obj = json?.data?.object ?? json?.data ?? json;
  return new Set((obj.fields ?? []).map((f) => f.name));
}

async function main() {
  const cache = new Map();
  for (const field of FIELDS) {
    if (!cache.has(field.objectMetadataId)) {
      cache.set(field.objectMetadataId, await existingFieldNames(field.objectMetadataId));
    }
    const names = cache.get(field.objectMetadataId);
    if (names.has(field.name)) {
      console.log(`= skip ${field.name} (already exists)`);
      continue;
    }
    const res = await fetch(`${BASE}/rest/metadata/fields`, {
      method: "POST",
      headers,
      body: JSON.stringify(field),
    });
    if (!res.ok) {
      console.error(`x create ${field.name} failed: ${res.status} ${await res.text()}`);
      process.exitCode = 1;
    } else {
      console.log(`+ created ${field.name} (${field.type})`);
    }
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
