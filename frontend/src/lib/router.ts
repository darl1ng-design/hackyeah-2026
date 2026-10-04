// Hash router: `#/path?query`. ponytail: regex table + hashchange; swap for react-router if layouts nest.
import { useSyncExternalStore } from "react";

export type RouteName =
  | "start"
  | "katalog"
  | "inn"
  | "dopasuj"
  | "wynik"
  | "pomysly"
  | "nowy"
  | "pomysl"
  | "zasoby"
  | "konto"
  | "powiad"
  | "praporty"
  | "ppomysly"
  | "ppomysl"
  | "wiedza"
  | "edytor"
  | "trendy"
  | "nabory"
  | "nabor"
  | "wnioski"
  | "mentorzy"
  | "mentor"
  | "adaptuj"
  | "testerQueue"
  | "grantQueue"
  | "mentorQueue"
  | "middlemanQueue"
  | "grantAdmin"
  | "notfound";

export type Route = {
  name: RouteName;
  params: Record<string, string>;
  query: Record<string, string>;
  path: string;
};

const ROUTES: [RegExp, RouteName, string[]?, Record<string, string>?][] = [
  [/^\/$/, "start"],
  [/^\/innowacje$/, "katalog"],
  [/^\/innowacje\/(\d+)$/, "inn", ["id"]],
  [/^\/dopasuj$/, "dopasuj"],
  [/^\/dopasuj\/(\d+)$/, "wynik", ["id"]],
  [/^\/pomysly$/, "pomysly"],
  [/^\/pomysly\/nowy$/, "nowy"],
  [/^\/pomysly\/(\d+)$/, "pomysl", ["id"]],
  [/^\/zasoby$/, "zasoby"],
  [/^\/nabory$/, "nabory"],
  [/^\/nabory\/(\d+)$/, "nabor", ["id"]],
  [/^\/moje-wnioski$/, "wnioski"],
  [/^\/mentorzy$/, "mentorzy"],
  [/^\/mentorzy\/(\d+)$/, "mentor", ["id"]],
  [/^\/adaptuj(?:\/(\d+))?$/, "adaptuj", ["id"]],
  [/^\/logowanie$/, "konto"],
  [/^\/rejestracja$/, "konto", [], { tab: "register" }],
  [/^\/powiadomienia$/, "powiad"],
  [/^\/panel(\/raporty)?$/, "praporty"],
  [/^\/panel\/pomysly$/, "ppomysly"],
  [/^\/panel\/pomysly\/(\d+)$/, "ppomysl", ["id"]],
  [/^\/panel\/wiedza$/, "wiedza"],
  [
    /^\/panel\/wiedza\/(innowacja|zasob)\/(\d+|nowy)$/,
    "edytor",
    ["type", "id"],
  ],
  [/^\/panel\/trendy$/, "trendy"],
  [/^\/panel\/testerzy(?:\/(\d+))?$/, "testerQueue", ["id"]],
  [/^\/panel\/wnioski(?:\/(\d+))?$/, "grantQueue", ["id"]],
  [/^\/panel\/mentorzy(?:\/(\d+))?$/, "mentorQueue", ["id"]],
  [/^\/panel\/adaptacje$/, "middlemanQueue"],
  [/^\/panel\/nabory$/, "grantAdmin"],
];

export const AUTH_ROUTES: RouteName[] = ["nowy", "powiad", "nabor", "wnioski", "mentorzy", "mentor", "adaptuj"];
export const STAFF_ROUTES: RouteName[] = ["praporty", "ppomysly", "ppomysl", "testerQueue", "grantQueue", "mentorQueue", "middlemanQueue"];
export const ADMIN_ROUTES: RouteName[] = ["wiedza", "edytor", "trendy", "grantAdmin"];
export const isPanelRoute = (n: RouteName) =>
  STAFF_ROUTES.includes(n) || ADMIN_ROUTES.includes(n);

export function parse(hash: string): Route {
  const path = hash.replace(/^#/, "") || "/";
  const [p, qs] = path.split("?");
  const query = Object.fromEntries(new URLSearchParams(qs || ""));
  for (const [re, name, keys = [], extra = {}] of ROUTES) {
    const m = p.match(re);
    if (m)
      return {
        name,
        params: Object.fromEntries(keys.map((k, i) => [k, m[i + 1]])),
        query: { ...extra, ...query },
        path,
      };
  }
  return { name: "notfound", params: {}, query, path };
}

const subscribe = (cb: () => void) => {
  window.addEventListener("hashchange", cb);
  return () => window.removeEventListener("hashchange", cb);
};

/** Current hash, re-rendering on change. Parse with `parse()` (memoize on the string). */
export const useHash = () =>
  useSyncExternalStore(subscribe, () => location.hash);

export const go = (path: string) => {
  location.hash = path;
};

/** Merge `patch` into current query (empty values removed) and navigate. */
export function setQuery(
  route: Route,
  patch: Record<string, string | number | undefined>,
) {
  const q = new URLSearchParams();
  Object.entries({ ...route.query, ...patch }).forEach(
    ([k, v]) => v !== undefined && v !== "" && q.set(k, String(v)),
  );
  const s = q.toString();
  go(route.path.split("?")[0] + (s ? "?" + s : ""));
}

export const loginHref = (next: string) =>
  "/logowanie?next=" + encodeURIComponent(next);
