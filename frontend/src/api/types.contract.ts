import type { Area, MatchResponse, Notification } from "./types";

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2
  ? true
  : false;
type Expect<T extends true> = T;

type NotificationKindMatchesBackend = Expect<
  Equal<Notification["kind"], "NEW_IDEA" | "IDEA_REPLY" | "NEW_GRANT_APPLICATION" | "MENTOR_MESSAGE" | "GRANT_STATUS" | "TESTER_ACTIVITY">
>;
type NotificationTargetMatchesBackend = Expect<
  Equal<Notification["targetType"], "IDEA" | "MENTOR_CONVERSATION" | "GRANT_APPLICATION" | "TESTER_FEEDBACK">
>;
type NotificationIdeaIdAllowsGenericTargets = Expect<Equal<Notification["ideaId"], number | null>>;
type MatchAreaAllowsUnclassifiedReports = Expect<Equal<MatchResponse["area"], Area | null>>;

export type ApiContractChecks = [NotificationKindMatchesBackend, NotificationTargetMatchesBackend,
  NotificationIdeaIdAllowsGenericTargets, MatchAreaAllowsUnclassifiedReports];
