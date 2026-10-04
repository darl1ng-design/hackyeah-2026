import { useState, type ReactNode } from "react";
import { Button, IconButton, Logo } from "../../components/ds";
import { go, type Route } from "../../lib/router";
import { useMedia } from "../../lib/useMedia";
import { useSession } from "../../session";
import s from "./PublicLayout.module.css";

const NAV = [
  ["katalog", "Katalog", "/innowacje"],
  ["dopasuj", "Dopasuj", "/dopasuj"],
  ["pomysly", "Pomysły", "/pomysly"],
  ["zasoby", "Zasoby", "/zasoby"],
  ["nabory", "Nabory", "/nabory"],
] as const;
const SECTION: Partial<Record<Route["name"], string>> = {
  katalog: "katalog",
  inn: "katalog",
  dopasuj: "dopasuj",
  wynik: "dopasuj",
  pomysly: "pomysly",
  pomysl: "pomysly",
  nowy: "pomysly",
  zasoby: "zasoby",
  nabory: "nabory",
  nabor: "nabory",
  mentorzy: "mentorzy",
  mentor: "mentorzy",
  adaptuj: "adaptuj",
};

export function PublicLayout({
  route,
  children,
}: {
  route: Route;
  children: ReactNode;
}) {
  const { me, role, notifs, logout } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);
  const tiny = useMedia("(max-width: 439px)");
  const narrow = useMedia("(max-width: 959px)");
  // Close the mobile menu on navigation and when the viewport leaves the mobile breakpoint.
  const [lastPath, setLastPath] = useState(route.path);
  if (lastPath !== route.path || (menuOpen && !narrow)) {
    setLastPath(route.path);
    setMenuOpen(false);
  }
  const sec = SECTION[route.name];
  const unread = notifs.filter((n) => !n.read).length;
  const links = NAV.map(([k, label, href]) => (
    <a key={k} href={"#" + href} aria-current={sec === k ? "page" : undefined}>
      {label}
    </a>
  ));
  const skip = () => document.querySelector<HTMLElement>("main")?.focus();

  return (
    <div lang="pl" className={s.page}>
      <button className={s.skip} onClick={skip}>
        Przejdź do treści
      </button>
      <header className={s.header}>
        <div className={s.topbar}>
          <div className={s.topbarInner}>
            {me ? (
              <>
                <span className={s.email}>{me.username}</span>
                {(role === "STAFF" || role === "ADMIN") && (
                  <a href="#/panel/raporty" className={s.topLink}>
                    Panel pracownika
                  </a>
                )}
                <button className={`link-btn ${s.topLink}`} onClick={logout}>
                  Wyloguj
                </button>
              </>
            ) : (
              <>
                <a href="#/logowanie" className={s.topLink}>
                  Zaloguj się
                </a>
                <a href="#/rejestracja" className={s.topLink}>
                  Załóż konto
                </a>
              </>
            )}
          </div>
        </div>
        <div className={s.bar}>
          <a
            href="#/"
            className={s.logo}
            aria-label="Kompas Małopolski — strona główna"
          >
            <Logo name="Kompas Małopolski" size={40} showName={!tiny} />
          </a>
          <nav aria-label="Główna nawigacja" className={s.nav}>
            {links}
          </nav>
          <span className={s.spacer} />
          <IconButton
            icon="search"
            label="Szukaj w katalogu"
            onClick={() => go("/innowacje")}
          />
          {me && (
            <span className={s.bell}>
              <IconButton
                icon="bell"
                label={
                  unread
                    ? `Powiadomienia, nieprzeczytane: ${unread}`
                    : "Powiadomienia"
                }
                onClick={() => go("/powiadomienia")}
              />
              {unread > 0 && (
                <span aria-hidden="true" className={s.badge}>
                  {unread}
                </span>
              )}
            </span>
          )}
          <span className={s.wideOnly}>
            <Button iconLeft="lightbulb" onClick={() => go("/pomysly/nowy")}>
              Zgłoś pomysł
            </Button>
          </span>
          <span className={s.narrowOnly}>
            <IconButton
              icon={menuOpen ? "x" : "menu"}
              label="Menu"
              onClick={() => setMenuOpen((o) => !o)}
            />
          </span>
        </div>
        {menuOpen && (
          <nav aria-label="Menu" className={s.menu}>
            {links}
            <div className={s.menuCta}>
              <Button
                iconLeft="lightbulb"
                fullWidth
                onClick={() => go("/pomysly/nowy")}
              >
                Zgłoś pomysł
              </Button>
            </div>
          </nav>
        )}
      </header>

      <main tabIndex={-1} className={s.main}>
        {children}
      </main>

      <footer className={s.footer}>
        <div className={s.footerInner}>
          <div
            className="stack"
            style={{ "--gap": "var(--space-4)" } as React.CSSProperties}
          >
            <Logo variant="reversed" name="Kompas Małopolski" size={40} />
            <p className={s.footerNote}>
              Baza innowacji społecznych, dopasowanie rozwiązań i bank pomysłów
              mieszkańców.
            </p>
          </div>
          <div className={s.footerCol}>
            <span className={s.footerHead}>Serwis</span>
            <a href="#/innowacje">Katalog innowacji</a>
            <a href="#/dopasuj">Dopasuj rozwiązania</a>
            <a href="#/pomysly">Pomysły</a>
            <a href="#/zasoby">Zasoby</a>
            <a href="#/nabory">Otwarte nabory</a>
            <a href="#/mentorzy">Porozmawiaj z ekspertem</a>
            <a href="#/adaptuj">Adaptuj innowację do usługi</a>
          </div>
        </div>
        <div className={s.copy}>© 2026 Kompas Małopolski</div>
      </footer>
    </div>
  );
}
