// Single fetch wrapper for the Spring backend: session cookie + CSRF + JSON + errors.
import type * as T from "./types";

export class ApiError extends Error {
  status: number;
  fields: Record<string, string>;
  constructor(
    status: number,
    message: string,
    fields: Record<string, string> = {},
  ) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

const FALLBACK: Record<number, string> = {
  400: "Popraw zaznaczone pola.",
  401: "Zaloguj się, żeby kontynuować.",
  403: "Nie masz dostępu do tej części.",
  404: "Nie znaleziono.",
  413: "Plik jest za duży.",
  429: "Za dużo prób. Spróbuj za minutę.",
  503: "Usługa jest chwilowo niedostępna. Spróbuj ponownie za chwilę.",
};

let csrf: { token: string; headerName: string } | null = null;

async function csrfHeader(): Promise<Record<string, string>> {
  csrf ??= await request<{ token: string; headerName: string }>(
    "GET",
    "/api/v1/csrf",
  );
  return { [csrf.headerName]: csrf.token };
}

type Query = Record<string, string | number | boolean | undefined>;

async function request<R>(
  method: string,
  path: string,
  body?: unknown,
  query?: Query,
): Promise<R> {
  const qs = query
    ? "?" +
      new URLSearchParams(
        Object.entries(query)
          .filter(([, v]) => v !== undefined && v !== "")
          .map(([k, v]) => [k, String(v)]),
      )
    : "";
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") Object.assign(headers, await csrfHeader());
  const multipart = body instanceof FormData; // browser sets the multipart boundary itself
  if (body !== undefined && !multipart) headers["Content-Type"] = "application/json";

  let res: Response;
  try {
    res = await fetch(path + qs, {
      method,
      headers,
      credentials: "include",
      body: body === undefined || multipart ? (body as FormData | undefined) : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(
      0,
      "Brak połączenia z serwerem. Spróbuj ponownie za chwilę.",
    );
  }
  if (res.status === 403 && method !== "GET") csrf = null; // token may have rotated; next call refetches
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new ApiError(
      res.status,
      data.message || FALLBACK[res.status] || "Coś poszło nie tak.",
      data.fields || {},
    );
  }
  return res.status === 204 ? (undefined as R) : res.json();
}

// Spring formLogin/logout: form-encoded POST answered by a redirect we don't follow.
async function form(path: string, data: Record<string, string>) {
  const body = new URLSearchParams(data);
  await fetch(path, {
    method: "POST",
    headers: await csrfHeader(),
    credentials: "include",
    body,
    redirect: "manual",
  });
  csrf = null; // session id changed → new CSRF token
}

const get = <R>(p: string, q?: Query) => request<R>("GET", p, undefined, q);

export const api = {
  me: () => get<T.Me>("/api/v1/me"),
  login: async (username: string, password: string) => {
    await form("/login", { username, password });
    try {
      return await api.me();
    } catch {
      throw new ApiError(401, "Nieprawidłowy e-mail lub hasło.");
    }
  },
  logout: () => form("/logout", {}),
  register: (b: T.RegisterRequest) =>
    request<unknown>("POST", "/api/v1/register", b),

  areas: () => get<T.Area[]>("/api/v1/areas"),
  regions: () => get<T.Region[]>("/api/v1/regions"),
  innovations: (q: {
    q?: string;
    areaId?: number;
    region?: string;
    page?: number;
    size?: number;
    sort?: string;
  }) => get<T.Page<T.Innovation>>("/api/v1/innovations", q),
  innovation: (id: number) => get<T.Innovation>(`/api/v1/innovations/${id}`),
  match: (b: T.MatchRequest) =>
    request<T.MatchResponse>("POST", "/api/v1/matches", b),
  getMatch: (id: number) => get<T.MatchResponse>(`/api/v1/matches/${id}`),

  ideas: (mine = false) =>
    get<T.Idea[]>("/api/v1/ideas", mine ? { mine: true } : undefined),
  idea: (id: number) => get<T.Idea>(`/api/v1/ideas/${id}`),
  replies: (id: number) => get<T.Reply[]>(`/api/v1/ideas/${id}/replies`),
  createIdea: (b: T.IdeaRequest) => request<T.Idea>("POST", "/api/v1/ideas", b),
  parseIdea: (text: string) =>
    request<Required<T.IdeaRequest>>("POST", "/api/v1/ideas/assistant/parse", { text }),
  transcriptionHealth: () => get<{ available: boolean }>("/api/v1/transcribe/health"),
  transcribe: (audio: Blob, language = "pl") => {
    const f = new FormData();
    f.append("file", audio, "nagranie");
    f.append("language", language);
    return request<{ text: string }>("POST", "/api/v1/transcribe", f);
  },
  assistant: (b: T.AssistantRequest) =>
    request<{ reply: string }>("POST", "/api/v1/ideas/assistant", b),
  resources: () => get<T.Resource[]>("/api/v1/resources"),
  notifications: () => get<T.Notification[]>("/api/v1/notifications"),
  markRead: (id: number) =>
    request<T.Notification>("PATCH", `/api/v1/notifications/${id}/read`),

  reports: () => get<T.Report[]>("/api/v1/reports"),
  staffIdeas: (status: T.Moderation) =>
    get<T.Idea[]>("/api/v1/staff/ideas", { status }),
  reply: (id: number, body: string) =>
    request<T.Reply>("POST", `/api/v1/staff/ideas/${id}/replies`, { body }),
  moderate: (id: number, status: T.Moderation) =>
    request<T.Idea>("PATCH", `/api/v1/admin/ideas/${id}/moderation`, {
      status,
    }),
  setReportStatus: (id: number, status: T.ReportStatus) =>
    request<T.Report>("PATCH", `/api/v1/admin/reports/${id}/status`, {
      status,
    }),

  adminInnovations: (q: { page?: number; size?: number }) =>
    get<T.Page<T.Innovation>>("/api/v1/admin/innovations", q),
  adminInnovation: (id: number) =>
    get<T.Innovation>(`/api/v1/admin/innovations/${id}`),
  saveInnovation: (id: number | null, b: T.InnovationUpdate) =>
    id
      ? request<T.Innovation>("PUT", `/api/v1/admin/innovations/${id}`, b)
      : request<T.Innovation>("POST", "/api/v1/admin/innovations", b),
  saveArea: (id: number | null, b: { name: string; description: string }) =>
    id
      ? request<T.Area>("PUT", `/api/v1/admin/areas/${id}`, b)
      : request<T.Area>("POST", "/api/v1/admin/areas", b),
  adminResources: () => get<T.Resource[]>("/api/v1/admin/resources"),
  adminResource: (id: number) =>
    get<T.Resource>(`/api/v1/admin/resources/${id}`),
  saveResource: (id: number | null, b: T.ResourceUpdate) =>
    id
      ? request<T.Resource>("PUT", `/api/v1/admin/resources/${id}`, b)
      : request<T.Resource>("POST", "/api/v1/admin/resources", b),
  trends: (q: { from?: string; to?: string }) =>
    get<T.Trends>("/api/v1/admin/trends", q),
};
