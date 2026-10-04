// Polish UI labels for backend enums (from the design prototype's hub-api.js).
import type { Moderation, ReportStatus } from "../api/types";
import type { ReportBadge, Tone } from "../components/ds";

export const LABELS = {
  innovationStatus: {
    ROZWOJ: "W rozwoju",
    TESTOWANA: "Testowana",
    WDROZONA: "Wdrożona",
  },
  stage: {
    MYSL: "Myśl",
    PROTOTYP: "Prototyp",
    TESTY: "Testy",
    WDROZENIE: "Wdrożenie",
  },
  stageHint: {
    MYSL: "Masz pomysł, jeszcze go nie sprawdzałeś",
    PROTOTYP: "Przygotowujesz pierwszą wersję",
    TESTY: "Sprawdzasz rozwiązanie z odbiorcami",
    WDROZENIE: "Rozwiązanie działa na stałe",
  },
  moderation: {
    PENDING: "Czeka na sprawdzenie",
    APPROVED: "Zatwierdzony",
    REJECTED: "Odrzucony",
  },
  reportStatus: {
    NOWE: "Nowe",
    PRZYPISANE: "Przypisane",
    W_REALIZACJI: "W realizacji",
    ZAMKNIETE: "Zamknięte",
  },
  resourceKind: {
    BIBLIOTEKA: "Biblioteka",
    RAPORTY: "Raporty",
    STATYSTYKI: "Statystyki",
    MAPA: "Mapa",
    PUBLIKACJE: "Publikacje",
    CANVAS: "Canvas",
  },
  role: { MEMBER: "Mieszkaniec", STAFF: "Pracownik", ADMIN: "Administrator" },
} as const;

export const MOD_TONE: Record<Moderation, Tone> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "neutral",
};
export const REPORT_BADGE: Record<ReportStatus, ReportBadge> = {
  NOWE: "new",
  PRZYPISANE: "forwarded",
  W_REALIZACJI: "progress",
  ZAMKNIETE: "resolved",
};
export const INN_STATUS_TONE: Record<string, Tone> = {
  ROZWOJ: "info",
  TESTOWANA: "warning",
  WDROZONA: "success",
};

/** `{a:'A'}` → `[{value:'a',label:'A'}]` for Select/RadioGroup. */
export const options = (m: Record<string, string>) =>
  Object.entries(m).map(([value, label]) => ({ value, label }));
