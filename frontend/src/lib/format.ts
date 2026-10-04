// Polish date/number formatting (from the design prototype's HubFormat).
const MONTHS = [
  "sty",
  "lut",
  "mar",
  "kwi",
  "maj",
  "cze",
  "lip",
  "sie",
  "wrz",
  "paź",
  "lis",
  "gru",
];
const MONTHS_FULL = [
  "Styczeń",
  "Luty",
  "Marzec",
  "Kwiecień",
  "Maj",
  "Czerwiec",
  "Lipiec",
  "Sierpień",
  "Wrzesień",
  "Październik",
  "Listopad",
  "Grudzień",
];
const pad = (n: number) => String(n).padStart(2, "0");

export const formatDate = (iso: string) => {
  if (!iso) return "";
  const d = new Date(iso);
  return `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}`;
};
export const formatDateTime = (iso: string) => {
  if (!iso) return "";
  const d = new Date(iso);
  return `${d.getDate()} ${MONTHS[d.getMonth()]}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
export const formatMonth = (ym: string) => {
  const [y, m] = ym.split("-");
  return `${MONTHS_FULL[+m - 1]} ${y}`;
};
export const reportNo = (id: number) =>
  `RD/2026/${String(id).padStart(5, "0")}`;
export const similarityLabel = (s: number) =>
  s >= 0.7 ? "Bardzo podobne" : s >= 0.45 ? "Podobne" : "Luźno powiązane";

/** Polish plural: plural(5,'pomysł','pomysły','pomysłów') → 'pomysłów'. */
export const plural = (n: number, one: string, few: string, many: string) =>
  n === 1
    ? one
    : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)
      ? few
      : many;

export const host = (url: string) => {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
};

/** Only http(s) URLs may reach an href (blocks `javascript:` etc. from stored data). */
export const safeUrl = (url: string | null | undefined) => {
  try {
    const p = new URL(url ?? '').protocol;
    return p === 'http:' || p === 'https:' ? url! : undefined;
  } catch {
    return undefined;
  }
};
