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
  area: Area | null;
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

export type NotificationKind =
  | "NEW_IDEA"
  | "IDEA_REPLY"
  | "NEW_GRANT_APPLICATION"
  | "MENTOR_MESSAGE"
  | "GRANT_STATUS"
  | "TESTER_ACTIVITY";
export type NotificationTargetType =
  | "IDEA"
  | "MENTOR_CONVERSATION"
  | "GRANT_APPLICATION"
  | "TESTER_FEEDBACK";
export type Notification = {
  id: number;
  ideaId: number | null;
  kind: NotificationKind;
  targetType: NotificationTargetType;
  targetId: number;
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


export type WorkflowRecord<T extends Record<string, unknown> = Record<string, unknown>> = {
  id: number;
  module: string;
  referenceId: number | null;
  status: string;
  title: string;
  payload: T;
  author: string | null;
  createdAt: string;
  updatedAt: string;
};
export type TesterFeedbackRequest = {
  interested: true;
  rating?: number;
  feedback?: string;
  suggestion?: string;
};
export type GrantFieldType = "TEXT" | "TEXTAREA" | "NUMBER" | "SELECT" | "CHECKBOX";
export type GrantCallStatus = "DRAFT" | "OPEN" | "CLOSED";
export type GrantApplicationStatus = "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";
export type GrantField = {
  key: string;
  label: string;
  type: GrantFieldType;
  required: boolean;
  options?: string[];
};
export type GrantCall = WorkflowRecord<{
  description: string;
  opensAt: string;
  closesAt: string;
  fields: GrantField[];
}>;
export type GrantApplication = WorkflowRecord<{
  answers: Record<string, string>;
  formSnapshot: GrantCall["payload"];
}>;
export type GrantCallRequest = {
  title: string;
  description?: string;
  opensAt: string;
  closesAt: string;
  status: GrantCallStatus;
  fields: GrantField[];
};
export type MentorMessage = {
  id: number;
  author: string;
  authorRole: Role;
  body: string;
  createdAt: string;
};
export type MentorConversation = {
  id: number;
  subject: string;
  status: "OPEN" | "CLOSED";
  author: string;
  createdAt: string;
  messages: MentorMessage[];
};
export type MiddlemanPlan = WorkflowRecord<{
  draft: true;
  label: string;
  innovationId: number;
  innovationTitle: string;
  input: { institution: string; targetGroup: string; need: string; constraints: string };
  plan: {
    serviceName: string;
    summary: string;
    targetGroup: string[];
    steps: string[];
    resources: string[];
    partners: string[];
    risks: string[];
    successMeasures: string[];
  };
}>;
