import { z } from "zod";

/** Tags describing which fields enrichment managed to fill (mirrors Twenty's `Enriched` multi-select). */
export const ENRICHED_TAGS = [
  "EMAIL",
  "PHONE",
  "LINKEDIN",
  "JOB_TITLE",
  "CITY",
  "COMPANY",
  "AVATAR",
] as const;
export type EnrichedTag = (typeof ENRICHED_TAGS)[number];

/** What /api/enrich returns to the app for review. */
export interface EnrichResult {
  prmId: string;
  transcript: string;
  linkedinUrl: string;
  headline: string;
  avatarUrl: string;
  companyDomain: string;
  summary: string;
  enriched: EnrichedTag[];
}

/** Body the app sends to /api/commit after the user confirms/edits the enrichment. */
export const CommitSchema = z.object({
  prmId: z.string().min(1),
  firstName: z.string().default(""),
  lastName: z.string().default(""),
  company: z.string().default(""),
  companyDomain: z.string().default(""),
  number: z.string().default(""),
  headline: z.string().default(""),
  linkedinUrl: z.string().default(""),
  avatarUrl: z.string().default(""),
  summary: z.string().default(""),
  note: z.string().default(""),
  events: z.string().default(""),
  sources: z.string().default(""),
  enriched: z.array(z.string()).default([]),
});
export type CommitBody = z.infer<typeof CommitSchema>;

export interface CommitResult {
  prmId: string;
  twentyId: string | null;
  twentyUrl: string | null;
  googleResourceName: string | null;
}
