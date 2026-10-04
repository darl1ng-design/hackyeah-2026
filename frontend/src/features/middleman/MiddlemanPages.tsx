import { useState, type FormEvent } from "react";
import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import { Button, Select, Textarea, TextField } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { errorMessage } from "../../lib/errors";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import s from "../panel/Panel.module.css";

export function MiddlemanPage({ route }: ScreenProps) {
  const selectedId = Number(route.params.id || 0);
  const catalogue = useApi(() => Promise.all([api.innovations({ size: 100 }), api.myMiddlemanPlans()]), []);
  const detail = useApi(() => selectedId ? api.middlemanPlan(selectedId) : Promise.resolve(null), [selectedId]);
  const [innovationId, setInnovationId] = useState(""); const [institution, setInstitution] = useState("");
  const [targetGroup, setTargetGroup] = useState(""); const [need, setNeed] = useState(""); const [constraints, setConstraints] = useState("");
  const [errorText, setErrorText] = useState(""); const [busy, setBusy] = useState(false);
  if (!catalogue.data) return <div className="container"><PageState loading={catalogue.loading} error={catalogue.error} /></div>;
  const [catalog, plans] = catalogue.data;
  async function generate(event: FormEvent) {
    event.preventDefault(); setBusy(true); setErrorText("");
    try { const saved = await api.generateMiddlemanPlan({ innovationId: Number(innovationId), institution, targetGroup, need, constraints }); go(`/adaptuj/${saved.id}`); catalogue.reload(); }
    catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setBusy(false); }
  }
  const plan = selectedId ? detail.data : null;
  return <div className="container stack">
    <header className="stack"><h1 className="h1">Middleman innowacji</h1><p className="lead">Opisz potrzeby instytucji, a asystent przygotuje szkic usługi na podstawie wybranej innowacji. Każdy plan AI wymaga oceny człowieka.</p></header>
    {selectedId > 0 && !detail.data ? <PageState loading={detail.loading} error={detail.error} /> : plan ? <section className="box stack" style={{ padding: "var(--space-6)" }}>
      <p role="status"><strong>{String(plan.payload.label)}</strong></p><h2 className="h2">{plan.payload.plan.serviceName}</h2><p>{plan.payload.plan.summary}</p>
      {Object.entries({ "Grupa odbiorców": plan.payload.plan.targetGroup, "Kroki wdrożenia": plan.payload.plan.steps, "Potrzebne zasoby": plan.payload.plan.resources, "Partnerzy": plan.payload.plan.partners, "Ryzyka": plan.payload.plan.risks, "Miary powodzenia": plan.payload.plan.successMeasures }).map(([heading, items]) => <section key={heading}><h3 className="h3">{heading}</h3><ul>{items.map((item, index) => <li key={`${heading}-${index}`}>{item}</li>)}</ul></section>)}
      <a href="#/adaptuj">Przygotuj kolejny plan</a>
    </section> : <form className="box stack" style={{ padding: "var(--space-6)" }} onSubmit={generate}>
      <Select id="adapt-innovation" label="Istniejąca innowacja" required value={innovationId} options={catalog.content.map((item) => ({ value: String(item.id), label: item.title }))} placeholder="Wybierz z katalogu" onChange={(e) => setInnovationId(e.target.value)} />
      <TextField id="adapt-institution" label="Instytucja" required value={institution} onChange={(e) => setInstitution(e.target.value)} />
      <TextField id="adapt-target" label="Odbiorcy planowanej usługi" required value={targetGroup} onChange={(e) => setTargetGroup(e.target.value)} />
      <Textarea id="adapt-need" label="Potrzeba i cel wdrożenia" required maxLength={4000} rows={4} value={need} onChange={(e) => setNeed(e.target.value)} />
      <Textarea id="adapt-constraints" label="Ograniczenia, które trzeba uwzględnić" optional maxLength={4000} rows={3} value={constraints} onChange={(e) => setConstraints(e.target.value)} />
      <p className="small muted">Nie wpisuj danych osobowych ani wrażliwych. Treść zostanie wysłana do skonfigurowanego modelu AI i zapisana w Twoim koncie.</p>
      <p role="status" aria-live="polite">{busy ? "Generuję szkic AI…" : ""}</p>{errorText && <p role="alert">{errorText}</p>}<Button type="submit" disabled={busy || !catalog.content.length}>{busy ? "Tworzę szkic AI…" : "Przygotuj szkic usługi"}</Button>
    </form>}
    <section className="stack" aria-labelledby="plans-heading"><h2 id="plans-heading" className="h2">Zapisane szkice</h2>
      {!plans.length ? <p className="empty">Nie masz jeszcze szkiców Middlemana.</p> : <ul className="list">{plans.map((item) => <li className="list-row" key={item.id}>
        <span className={s.rowMain}><span className={s.rowTitle}>{item.title}</span><span className={s.sub}>{item.payload.innovationTitle}</span></span><a href={`#/adaptuj/${item.id}`}>Otwórz szkic</a>
      </li>)}</ul>}
    </section>
  </div>;
}

export function StaffMiddlemanPage() {
  const { data, error, loading } = useApi(() => api.staffMiddlemanPlans(), []);
  if (!data) return <PageState loading={loading} error={error} panel />;
  return <div className={s.page}><header className={s.head}><h1 className="h1">Plany Middlemana do przeglądu</h1><p className="muted">Treści wygenerowane przez AI są szkicami i wymagają weryfikacji.</p></header>
    {!data.length ? <p className="empty">Brak przygotowanych planów.</p> : <ul className="list">{data.map((plan) => <li className="list-row stack" key={plan.id}>
      <h2 className="h3">{plan.title}</h2><p className="small muted">{plan.author} · {plan.payload.innovationTitle}</p><p>{plan.payload.plan.summary}</p>
    </li>)}</ul>}
  </div>;
}
