import { useState, type FormEvent } from "react";
import { api } from "../../api/client";
import { Alert, Button, Tabs, TextField } from "../../components/ds";
import { errorMessage, fieldErrors } from "../../lib/errors";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./AuthPage.module.css";

type Tab = "login" | "register";
const TABS = [
  { value: "login", label: "Zaloguj się" },
  { value: "register", label: "Załóż konto" },
];

function noticeFor(next: string) {
  if (next.startsWith("/pomysly/nowy"))
    return "Zaloguj się albo załóż konto, żeby zgłosić pomysł. Katalog i dopasowanie działają bez konta.";
  if (next.startsWith("/panel"))
    return "Zaloguj się kontem pracownika, żeby otworzyć panel.";
  return "Zaloguj się, żeby kontynuować.";
}

export function AuthPage({ route }: ScreenProps) {
  const { setMe, showToast } = useSession();
  const next = route.query.next || "";
  const [tab, setTab] = useState<Tab>(
    route.query.tab === "register" ? "register" : "login",
  );
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [pass, setPass] = useState("");
  const [err, setErr] = useState<Record<string, string>>({});
  const [msg, setMsg] = useState("");
  const [sending, setSending] = useState(false);
  const isLogin = tab === "login";

  async function signIn(toast: string) {
    const me = await api.login(email.trim(), pass);
    setMe(me); // session effect refreshes notifications on `me` change
    showToast(toast, "success");
    go(next || (me.roles[0] === "MEMBER" ? "/" : "/panel/raporty"));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (sending) return;
    if (isLogin) {
      const v: Record<string, string> = {};
      if (!email.trim()) v.email = "Wpisz adres e-mail.";
      if (!pass) v.password = "Wpisz hasło.";
      if (Object.keys(v).length) return setErr(v);
    }
    setSending(true);
    setErr({});
    setMsg("");
    try {
      if (isLogin) await signIn("Zalogowano.");
      else {
        await api.register({
          email: email.trim(),
          password: pass,
          displayName: name,
        });
        await signIn("Konto założone. Jesteś zalogowany.");
      }
    } catch (ex) {
      const f = fieldErrors(ex);
      const hasFields = !isLogin && Object.keys(f).length > 0;
      setErr(isLogin ? {} : f);
      setMsg(hasFields ? "" : errorMessage(ex));
      setSending(false);
    }
  }

  return (
    <div className="container">
      <form className={s.form} onSubmit={submit} noValidate>
        <h1 className="h1">{isLogin ? "Zaloguj się" : "Załóż konto"}</h1>
        {next && <Alert tone="info">{noticeFor(next)}</Alert>}
        <Tabs
          tabs={TABS}
          value={tab}
          onChange={(v) => {
            setTab(v as Tab);
            setErr({});
            setMsg("");
          }}
        />
        {!isLogin && (
          <TextField
            id="r-name"
            label="Imię i nazwisko lub pseudonim"
            hint="Pokażemy je przy Twoich pomysłach."
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={err.displayName}
            autoComplete="name"
          />
        )}
        <TextField
          id={isLogin ? "a-email" : "r-email"}
          type="email"
          label="Adres e-mail"
          hint={isLogin ? undefined : "Posłuży do logowania."}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={err.email}
          autoComplete="email"
        />
        <TextField
          id={isLogin ? "a-pass" : "r-pass"}
          type="password"
          label="Hasło"
          hint={
            isLogin
              ? undefined
              : `Co najmniej 12 znaków. Wpisano: ${pass.length}.`
          }
          value={pass}
          onChange={(e) => setPass(e.target.value)}
          error={err.password}
          autoComplete={isLogin ? "current-password" : "new-password"}
        />
        {msg && <Alert tone="danger">{msg}</Alert>}
        <Button type="submit" size="lg" fullWidth disabled={sending}>
          {isLogin
            ? sending
              ? "Logujemy…"
              : "Zaloguj się"
            : sending
              ? "Zakładamy konto…"
              : "Załóż konto"}
        </Button>
        {!isLogin && (
          <p className="small muted">
            Konto jest potrzebne tylko do zgłaszania pomysłów. Katalog i
            dopasowanie działają bez logowania.
          </p>
        )}
      </form>
    </div>
  );
}
