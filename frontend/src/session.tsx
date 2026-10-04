// App-wide state: logged-in user, reference data (areas/regions), notifications, toast.
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { api } from "./api/client";
import { go } from "./lib/router";
import type { Area, Me, Notification, Region, Role } from "./api/types";

type ToastTone = "neutral" | "success" | "danger";
type ToastAction = { label: string; run: () => void };

type Session = {
  ready: boolean;
  me: Me | null;
  role: Role | null;
  areas: Area[];
  regions: Region[];
  notifs: Notification[];
  setMe: (me: Me | null) => void;
  refreshNotifs: () => void;
  setNotifs: (n: Notification[]) => void;
  toast: { msg: string; tone: ToastTone; action?: ToastAction } | null;
  showToast: (msg: string, tone?: ToastTone, action?: ToastAction) => void;
  closeToast: () => void;
  reloadAreas: () => void;
  logout: () => Promise<void>;
};

const Ctx = createContext<Session | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [me, setMe] = useState<Me | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [regions, setRegions] = useState<Region[]>([]);
  const [notifs, setNotifs] = useState<Notification[]>([]);
  const [toast, setToast] = useState<Session["toast"]>(null);
  const timer = useRef<number>(undefined);

  const refreshNotifs = useCallback(() => {
    api.notifications().then(setNotifs, () => setNotifs([]));
  }, []);
  const reloadAreas = useCallback(() => {
    api.areas().then(setAreas, () => {});
  }, []);

  useEffect(() => {
    // Reference data must not stay empty if the backend was briefly down: retry every 3s until it loads.
    // ponytail: fixed-interval retry, no backoff; fine for a booting backend.
    let timer: number | undefined;
    const loadRefs = () =>
      Promise.all([api.areas(), api.regions()]).then(
        ([a, r]) => {
          setAreas(a);
          setRegions(r);
        },
        () => {
          timer = window.setTimeout(loadRefs, 3000);
        },
      );
    Promise.all([loadRefs(), api.me().catch(() => null)]).then(([, m]) => {
      setMe(m);
      setReady(true);
    });
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (me) refreshNotifs();
  }, [me, refreshNotifs]);

  const showToast = useCallback((msg: string, tone: ToastTone = "neutral", action?: ToastAction) => {
    clearTimeout(timer.current);
    setToast({ msg, tone, action });
    timer.current = window.setTimeout(() => setToast(null), 4500);
  }, []);

  const logout = async () => {
    await api.logout().catch(() => {});
    setMe(null);
    setNotifs([]);
    showToast("Wylogowano.");
    go("/");
  };

  const value: Session = {
    ready,
    me,
    role: me?.roles[0] ?? null,
    areas,
    regions,
    notifs,
    setMe,
    refreshNotifs,
    setNotifs,
    toast,
    showToast,
    closeToast: () => setToast(null),
    reloadAreas,
    logout,
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSession() {
  const s = useContext(Ctx);
  if (!s) throw new Error("useSession outside SessionProvider");
  return s;
}
