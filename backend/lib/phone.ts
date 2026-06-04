/**
 * Split a raw phone string into the parts Twenty requires:
 * national number, calling code (e.g. "+49"), ISO country (e.g. "DE").
 * Ported from the original n8n "Create Twenty Person" expression.
 */
const CALLING_CODE_TO_ISO: Record<string, string> = {
  "1": "US", "7": "RU", "20": "EG", "27": "ZA", "30": "GR", "31": "NL", "32": "BE",
  "33": "FR", "34": "ES", "36": "HU", "39": "IT", "40": "RO", "41": "CH", "43": "AT",
  "44": "GB", "45": "DK", "46": "SE", "47": "NO", "48": "PL", "49": "DE", "51": "PE",
  "52": "MX", "54": "AR", "55": "BR", "56": "CL", "57": "CO", "58": "VE", "60": "MY",
  "61": "AU", "62": "ID", "63": "PH", "64": "NZ", "65": "SG", "66": "TH", "81": "JP",
  "82": "KR", "84": "VN", "86": "CN", "90": "TR", "91": "IN", "92": "PK", "94": "LK",
  "98": "IR", "212": "MA", "213": "DZ", "216": "TN", "220": "GM", "221": "SN", "233": "GH",
  "234": "NG", "254": "KE", "351": "PT", "352": "LU", "353": "IE", "354": "IS", "355": "AL",
  "356": "MT", "357": "CY", "358": "FI", "359": "BG", "370": "LT", "371": "LV", "372": "EE",
  "373": "MD", "374": "AM", "375": "BY", "380": "UA", "381": "RS", "385": "HR", "386": "SI",
  "387": "BA", "389": "MK", "420": "CZ", "421": "SK", "423": "LI", "852": "HK", "880": "BD",
  "886": "TW", "961": "LB", "962": "JO", "964": "IQ", "965": "KW", "966": "SA", "968": "OM",
  "971": "AE", "972": "IL", "973": "BH", "974": "QA", "976": "MN", "977": "NP", "994": "AZ",
  "995": "GE", "998": "UZ",
};

export interface PhoneParts {
  primaryPhoneNumber: string;
  primaryPhoneCallingCode: string;
  primaryPhoneCountryCode: string;
}

export function splitPhone(input: string): PhoneParts | null {
  const raw = (input || "").replace(/[^\d+]/g, "");
  if (!raw) return null;

  let calling = "";
  let iso = "";
  let national = raw;

  if (raw.startsWith("+")) {
    const digits = raw.slice(1);
    for (const len of [3, 2, 1]) {
      const code = digits.slice(0, len);
      if (CALLING_CODE_TO_ISO[code]) {
        calling = "+" + code;
        iso = CALLING_CODE_TO_ISO[code];
        national = digits.slice(len);
        break;
      }
    }
    if (!calling) national = digits;
  }

  if (!national) return null;
  return {
    primaryPhoneNumber: national,
    primaryPhoneCallingCode: calling,
    primaryPhoneCountryCode: iso,
  };
}
