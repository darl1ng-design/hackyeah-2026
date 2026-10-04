// Route name → screen component. Each screen: `({ route }: ScreenProps) => JSX`.
import type { ComponentType } from 'react';
import type { Route, RouteName } from './lib/router';
import { StartPage } from './features/start/StartPage';
import { CatalogPage } from './features/innovations/CatalogPage';
import { InnovationPage } from './features/innovations/InnovationPage';
import { MatchPage } from './features/match/MatchPage';
import { MatchResultPage } from './features/match/MatchResultPage';
import { IdeasPage } from './features/ideas/IdeasPage';
import { IdeaPage } from './features/ideas/IdeaPage';
import { NewIdeaPage } from './features/ideas/NewIdeaPage';
import { ResourcesPage } from './features/resources/ResourcesPage';
import { AuthPage } from './features/account/AuthPage';
import { NotificationsPage } from './features/account/NotificationsPage';
import { ReportsPage } from './features/panel/ReportsPage';
import { QueuePage } from './features/panel/QueuePage';
import { QueueIdeaPage } from './features/panel/QueueIdeaPage';
import { KnowledgePage } from './features/panel/KnowledgePage';
import { EditorPage } from './features/panel/EditorPage';
import { TrendsPage } from './features/panel/TrendsPage';
import { TesterQueuePage } from './features/testing/TesterQueuePage';
import { GrantCallsPage, MyApplicationsPage, StaffGrantApplicationsPage, AdminGrantCallsPage } from './features/grants/GrantPages';
import { MentorHomePage, MentorDetailPage, MentorQueuePage } from './features/mentors/MentorPages';
import { MiddlemanPage, StaffMiddlemanPage } from './features/middleman/MiddlemanPages';

export type ScreenProps = { route: Route };

export const screens: Record<Exclude<RouteName, 'notfound'>, ComponentType<ScreenProps>> = {
  start: StartPage,
  katalog: CatalogPage,
  inn: InnovationPage,
  dopasuj: MatchPage,
  wynik: MatchResultPage,
  pomysly: IdeasPage,
  nowy: NewIdeaPage,
  pomysl: IdeaPage,
  zasoby: ResourcesPage,
  konto: AuthPage,
  powiad: NotificationsPage,
  praporty: ReportsPage,
  ppomysly: QueuePage,
  ppomysl: QueueIdeaPage,
  wiedza: KnowledgePage,
  edytor: EditorPage,
  trendy: TrendsPage,
  nabory: GrantCallsPage,
  nabor: GrantCallsPage,
  wnioski: MyApplicationsPage,
  mentorzy: MentorHomePage,
  mentor: MentorDetailPage,
  adaptuj: MiddlemanPage,
  testerQueue: TesterQueuePage,
  grantQueue: StaffGrantApplicationsPage,
  mentorQueue: MentorQueuePage,
  middlemanQueue: StaffMiddlemanPage,
  grantAdmin: AdminGrantCallsPage,
};
