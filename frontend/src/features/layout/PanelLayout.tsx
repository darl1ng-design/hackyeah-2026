import { useEffect, useState, type ReactNode } from "react";
import { api } from "../../api/client";
import { Icon, Logo } from "../../components/ds";
import { LABELS } from "../../lib/labels";
import type { Route } from "../../lib/router";
import { useSession } from "../../session";
import s from "./PanelLayout.module.css";

const SECTION: Partial<Record<Route["name"], string>> = {
  praporty: "praporty",
  ppomysly: "ppomysly",
  ppomysl: "ppomysly",
  wiedza: "wiedza",
  edytor: "wiedza",
  trendy: "trendy",
  testerQueue: "testerzy",
  grantQueue: "wnioski",
  mentorQueue: "mentorzy",
  middlemanQueue: "adaptacje",
  grantAdmin: "nabory",
};

export function PanelLayout({
  route,
  children,
}: {
  route: Route;
  children: ReactNode;
}) {
  const { me, role, logout } = useSession();
  const [pending, setPending] = useState(0);

  // Pending-ideas badge, refreshed on every panel navigation (moderation changes it).
  useEffect(() => {
    api.staffIdeas("PENDING").then(
      (l) => setPending(l.length),
      () => {},
    );
  }, [route.path]);

  const sec = SECTION[route.name];
  const nav = [
    { k: "praporty", label: "Raporty", icon: "inbox", href: "/panel/raporty" },
    {
      k: "ppomysly",
      label: "Pomysły",
      icon: "lightbulb",
      href: "/panel/pomysly",
      count: pending,
    },
    { k: "testerzy", label: "Testerzy", icon: "flask-conical", href: "/panel/testerzy" },
    { k: "wnioski", label: "Wnioski grantowe", icon: "file-text", href: "/panel/wnioski" },
    { k: "mentorzy", label: "Rozmowy", icon: "message-circle", href: "/panel/mentorzy" },
    { k: "adaptacje", label: "Middleman", icon: "workflow", href: "/panel/adaptacje" },
    ...(role === "ADMIN"
      ? [
          {
            k: "nabory",
            label: "Nabory",
            icon: "file-plus-2",
            href: "/panel/nabory",
          },
          {
            k: "wiedza",
            label: "Baza wiedzy",
            icon: "book-open",
            href: "/panel/wiedza",
          },
          {
            k: "trendy",
            label: "Trendy",
            icon: "trending-up",
            href: "/panel/trendy",
          },
        ]
      : []),
  ];
  const links = nav.map((p) => (
    <a
      key={p.k}
      href={"#" + p.href}
      aria-current={sec === p.k ? "page" : undefined}
      className={s.navLink}
    >
      <Icon name={p.icon} size={20} />
      <span className={s.navLabel}>{p.label}</span>
      {!!p.count && <span className={s.count}>{p.count}</span>}
    </a>
  ));
  

  return (
    <div lang="pl" className={s.shell}>
      <aside className={s.sidebar}>
        <a href="#/panel/raporty" className={s.logo}>
          <Logo
            variant="reversed"
            name="Kompas Małopolski"
            tagline="Panel pracownika"
            size={36}
          />
        </a>
        <nav aria-label="Panel" className={s.nav}>
          {links}
        </nav>
        <div className={s.user}>
          <span className={s.role}>{role ? LABELS.role[role] : ""}</span>
          <span className={s.email}>{me?.username}</span>
          <a href="#/">Wróć do serwisu</a>
          <button className="link-btn" onClick={logout}>
            Wyloguj
          </button>
        </div>
      </aside>
      <header className={s.topbar}>
        <div className={s.topRow}>
          <a href="#/panel/raporty" className={s.logo}>
            <Logo variant="reversed" name="Panel pracownika" size={32} />
          </a>
          <span className={s.topLinks}>
            <a href="#/">Serwis</a>
            <button className="link-btn" onClick={logout}>
              Wyloguj
            </button>
          </span>
        </div>
        <nav aria-label="Panel" className={s.topNav}>
          {links}
        </nav>
      </header>
      <main tabIndex={-1} className={s.main}>
        {children}
      </main>
    </div>
  );
}
