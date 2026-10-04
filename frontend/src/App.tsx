import { useEffect, useMemo, type ReactNode } from "react";
import { ErrorView, Loading } from "./components/PageState";
import { ERR_403, ERR_404 } from "./lib/errors";
import { Toast } from "./components/ds";
import { PanelLayout } from "./features/layout/PanelLayout";
import { PublicLayout } from "./features/layout/PublicLayout";
import {
  ADMIN_ROUTES,
  AUTH_ROUTES,
  STAFF_ROUTES,
  go,
  isPanelRoute,
  loginHref,
  parse,
  useHash,
} from "./lib/router";
import { screens } from "./screens";
import { SessionProvider, useSession } from "./session";

export function App() {
  return (
    <SessionProvider>
      <Shell />
    </SessionProvider>
  );
}

function Shell() {
  const { ready, me, role, toast, closeToast } = useSession();
  const hash = useHash();
  const route = useMemo(() => parse(hash), [hash]);
  const needsLogin =
    ready &&
    !me &&
    (AUTH_ROUTES.includes(route.name) || isPanelRoute(route.name));

  useEffect(() => {
    if (needsLogin) go(loginHref(route.path));
  }, [needsLogin, route.path]);

  useEffect(() => {
    window.scrollTo(0, 0);
    document.querySelector<HTMLElement>("main")?.focus({ preventScroll: true });
  }, [route.path]);

  const panel = isPanelRoute(route.name) && !!me && role !== "MEMBER";
  const forbidden =
    (STAFF_ROUTES.includes(route.name) && role === "MEMBER") ||
    (ADMIN_ROUTES.includes(route.name) && role !== "ADMIN");
  // Public screens render their own .container; shell-level states need one.
  const boxed = (node: ReactNode) =>
    panel ? node : <div className="container">{node}</div>;

  let content: ReactNode;
  if (!ready || needsLogin) content = boxed(<Loading />);
  else if (route.name === "notfound")
    content = boxed(<ErrorView error={ERR_404()} />);
  else if (forbidden)
    content = boxed(<ErrorView error={ERR_403} panel={panel} />);
  else {
    const Screen = screens[route.name];
    // key: remount on path change so each screen starts with fresh local state
    content = <Screen key={route.path} route={route} />;
  }

  return (
    <>
      {panel ? (
        <PanelLayout route={route}>{content}</PanelLayout>
      ) : (
        <PublicLayout route={route}>{content}</PublicLayout>
      )}
      {toast && (
        <div className="toast-slot">
          <Toast tone={toast.tone} message={toast.msg} onClose={closeToast} />
        </div>
      )}
    </>
  );
}
