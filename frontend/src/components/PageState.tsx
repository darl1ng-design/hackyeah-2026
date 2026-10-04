// Loading / error states shared by all screens. 401 → login redirect, 403/404/other → error page.
import { useEffect } from "react";
import { ApiError } from "../api/client";
import { toPageError, type PageError } from "../lib/errors";
import { go, loginHref } from "../lib/router";
import { useSession } from "../session";
import { Button, Icon } from "./ds";

export function Loading() {
  return (
    <div role="status" className="lead">
      Wczytujemy dane…
    </div>
  );
}

export function ErrorView({
  error,
  panel = false,
}: {
  error: PageError;
  panel?: boolean;
}) {
  return (
    <div
      className="stack"
      style={
        {
          "--gap": "var(--space-4)",
          maxWidth: 760,
          alignItems: "flex-start",
        } as React.CSSProperties
      }
    >
      <Icon
        name={panel ? "shield-alert" : "circle-alert"}
        size={48}
        color="var(--red-600)"
      />
      <span className="mono muted">Błąd {error.status}</span>
      <h1 className="h1">{error.title}</h1>
      <p className="lead" style={{ color: "var(--gray-800)" }}>
        {error.text}
      </p>
      {panel ? (
        <Button
          variant="secondary"
          iconLeft="arrow-left"
          onClick={() => go("/panel/raporty")}
        >
          Wróć do raportów
        </Button>
      ) : (
        <Button
          variant="secondary"
          iconLeft="arrow-left"
          onClick={() => go("/")}
        >
          Wróć na stronę główną
        </Button>
      )}
    </div>
  );
}

/** Renders loading/error for a useApi() result; returns null when data is ready (caller renders content). */
export function PageState({
  loading,
  error,
  panel,
}: {
  loading: boolean;
  error: unknown;
  panel?: boolean;
}) {
  const { setMe } = useSession();
  const unauthorized = error instanceof ApiError && error.status === 401;
  useEffect(() => {
    if (unauthorized) {
      setMe(null);
      go(loginHref(location.hash.slice(1)));
    }
  }, [unauthorized, setMe]);
  if (loading || unauthorized) return <Loading />;
  if (error) return <ErrorView error={toPageError(error)} panel={panel} />;
  return null;
}
