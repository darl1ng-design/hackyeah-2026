// DTOs mirroring openapi.json (repo root). Hand-written; regenerate by hand when the contract changes.

export type Role = "MEMBER" | "STAFF" | "ADMIN";
export type InnovationStatus = "ROZWOJ" | "TESTOWANA" | "WDROZONA";
export type Stage = "MYSL" | "PROTOTYP" | "TESTY" | "WDROZENIE";
export type Moderation = "PENDING" | "APPROVED" | "REJECTED";
export type ReportStatus = "NOWE" | "PRZYPISANE" | "W_REALIZACJI" | "ZAMKNIETE";
export type ResourceKind =
  | "BIBLIOTEKA"
  | "RAPORTY"
  | "STATYSTYKI"
  | "MAPA"
  | "PUBLIKACJE"
  | "CANVAS";

export type Me = { username: string; roles: Role[] };
export type Area = { id: number; name: string; description: string };
export type Region = { code: string; label: string };
export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Innovation = {
  id: number;
  title: string;
  summary: string;
  description: string;
  targetGroup: string;
  status: InnovationStatus;
  region: string;
  videoUrl: string;
  sourceUrl: string;
  area: Area | null;
  createdAt: string;
  published: boolean;
};
export type InnovationUpdate = {
  title: string;
  summary: string;
  description: string;
  targetGroup: string;
  status: InnovationStatus;
  region: string;
  videoUrl: string;
  sourceUrl: string;
  areaId: number | null;
  published: boolean;
};

export type MatchRequest = {
  description: string;
  region?: string;
  authorName?: string;
};
export type MatchItem = {
  innovation: Innovation;
  why: string;
  similarity: number;
};
export type MatchResponse = {
  reportId: number;
  reportStatus: ReportStatus;
  area: Area;
  matches: MatchItem[];
};

export type Idea = {
  id: number;
  title: string;
  essence: string;
  targetGroup: string;
  stage: Stage;
  description: string;
  author: string;
  moderationStatus: Moderation;
  createdAt: string;
};
export type IdeaRequest = {
  title: string;
  essence?: string;
  targetGroup?: string;
  stage?: Stage;
  description?: string;
};
export type Reply = {
  id: number;
  ideaId: number;
  body: string;
  author: string;
  createdAt: string;
};
export type ChatMessage = { role: "USER" | "ASSISTANT"; content: string };
export type AssistantRequest = {
  message: string;
  history: ChatMessage[];
  ideaContext?: Partial<IdeaRequest>;
};

export type Notification = {
  id: number;
  ideaId: number;
  kind: "REPLY" | "MODERATION";
  title: string;
  createdAt: string;
  read: boolean;
};
export type Report = {
  id: number;
  description: string;
  region: string;
  authorName: string;
  status: ReportStatus;
  area: Area | null;
  createdAt: string;
};
export type Resource = {
  id: number;
  name: string;
  url: string;
  kind: ResourceKind;
  published: boolean;
};
export type ResourceUpdate = Omit<Resource, "id">;

export type TrendRow = { key: string; label: string; count: number };
export type Trends = {
  total: number;
  byArea: TrendRow[];
  byRegion: TrendRow[];
  byStatus: TrendRow[];
  byMonth: { month: string; count: number }[];
};
export type RegisterRequest = {
  displayName: string;
  email: string;
  password: string;
};
