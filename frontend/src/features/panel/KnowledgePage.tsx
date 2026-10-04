import { useState } from "react";
import { api } from "../../api/client";
import type { Area } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Badge, Button, Dialog, Icon, Pagination, Tabs, TextField, Textarea } from "../../components/ds";
import { errorMessage, fieldErrors } from "../../lib/errors";
import { INN_STATUS_TONE, LABELS } from "../../lib/labels";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./Panel.module.css";

const PAGE_SIZE = 10;
type Tab = "inn" | "areas" | "res";
type AreaDlg = { id: number | null; name: string; description: string; err: string; saving: boolean };

export function KnowledgePage({ route }: ScreenProps) {
  const { areas, regions, reloadAreas, showToast } = useSession();
  const tab: Tab = (["inn", "areas", "res"] as const).find((t) => t === route.query.tab) ?? "inn";
  const page = Math.max(1, +route.query.page || 1);
  const { data, error, loading } = useApi(
    () => Promise.all([api.adminInnovations({ page: page - 1, size: PAGE_SIZE }), api.adminResources()]),
    [page],
  );
  const [dlg, setDlg] = useState<AreaDlg | null>(null);

  if (!data) return <PageState loading={loading} error={error} panel />;
  const [inn, res] = data;
  const regionLabel = (c: string) => regions.find((r) => r.code === c)?.label ?? "";

  const openDlg = (a?: Area) =>
    setDlg({ id: a?.id ?? null, name: a?.name ?? "", description: a?.description ?? "", err: "", saving: false });

  const saveArea = async () => {
    if (!dlg) return;
    if (!dlg.name.trim()) return setDlg({ ...dlg, err: "Wpisz nazwę obszaru." });
    setDlg({ ...dlg, saving: true });
    try {
      await api.saveArea(dlg.id, { name: dlg.name.trim(), description: dlg.description });
      reloadAreas();
      setDlg(null);
      showToast(dlg.id ? "Obszar zapisany." : "Obszar dodany.", "success");
    } catch (e) {
      setDlg({ ...dlg, saving: false, err: fieldErrors(e).name || errorMessage(e) });
    }
  };

  const pub = (p: boolean, yes: string) => (
    <Badge tone={p ? "success" : "neutral"}>{p ? yes : "Szkic"}</Badge>
  );

  return (
    <div className={s.page}>
      <div className={s.headRow}>
        <div className={s.head}>
          <h1 className="h1">Baza wiedzy</h1>
          <p className="muted">
            Innowacje, obszary i zasoby widoczne w serwisie. Szkice widzi tylko administrator.
          </p>
        </div>
        {tab === "inn" && (
          <Button iconLeft="plus" onClick={() => go("/panel/wiedza/innowacja/nowy")}>
            Dodaj innowację
          </Button>
        )}
        {tab === "areas" && (
          <Button iconLeft="plus" onClick={() => openDlg()}>
            Dodaj obszar
          </Button>
        )}
        {tab === "res" && (
          <Button iconLeft="plus" onClick={() => go("/panel/wiedza/zasob/nowy")}>
            Dodaj zasób
          </Button>
        )}
      </div>
      <Tabs
        tabs={[
          { value: "inn", label: "Innowacje", count: inn.totalElements },
          { value: "areas", label: "Obszary", count: areas.length },
          { value: "res", label: "Zasoby", count: res.length },
        ]}
        value={tab}
        onChange={(v) => go("/panel/wiedza?tab=" + v)}
      />

      {tab === "inn" && (
        <>
          <div className="list">
            {inn.content.map((i) => (
              <a key={i.id} href={"#/panel/wiedza/innowacja/" + i.id} className="list-row">
                <span className={s.rowMain}>
                  <span className={s.rowTitle}>{i.title}</span>
                  <span className={s.sub}>
                    {i.area ? i.area.name : "Bez obszaru"} · {regionLabel(i.region)}
                  </span>
                </span>
                <Badge tone={INN_STATUS_TONE[i.status]}>{LABELS.innovationStatus[i.status]}</Badge>
                {pub(i.published, "Opublikowana")}
                <Icon name="chevron-right" size={20} />
              </a>
            ))}
            {!inn.content.length && <p className={s.emptyRow}>Brak innowacji.</p>}
          </div>
          {inn.totalPages > 1 && (
            <Pagination
              page={inn.page + 1}
              pageCount={inn.totalPages}
              onChange={(p) => go("/panel/wiedza?tab=inn&page=" + p)}
            />
          )}
        </>
      )}

      {tab === "areas" && (
        <div className="list">
          {areas.map((a) => (
            <div key={a.id} className="list-row">
              <span className={s.rowMain}>
                <span className={s.strong}>{a.name}</span>
                <span className={s.sub}>{a.description}</span>
              </span>
              <Button variant="outline" size="sm" iconLeft="pencil" onClick={() => openDlg(a)}>
                Edytuj
              </Button>
            </div>
          ))}
          {!areas.length && <p className={s.emptyRow}>Brak obszarów.</p>}
        </div>
      )}

      {tab === "res" && (
        <div className="list">
          {res.map((x) => (
            <a key={x.id} href={"#/panel/wiedza/zasob/" + x.id} className="list-row">
              <span className={s.rowMain}>
                <span className={s.rowTitle}>{x.name}</span>
                <span className={`${s.sub} ${s.url}`}>{x.url}</span>
              </span>
              <Badge tone="neutral">{LABELS.resourceKind[x.kind]}</Badge>
              {pub(x.published, "Opublikowany")}
              <Icon name="chevron-right" size={20} />
            </a>
          ))}
          {!res.length && <p className={s.emptyRow}>Brak zasobów.</p>}
        </div>
      )}

      <Dialog
        open={!!dlg}
        title={dlg?.id ? "Edytuj obszar" : "Dodaj obszar"}
        onClose={() => setDlg(null)}
      >
        {dlg && (
          <form
            className="stack"
            style={{ "--gap": "var(--space-4)" } as React.CSSProperties}
            onSubmit={(e) => {
              e.preventDefault();
              saveArea();
            }}
          >
            <TextField
              id="dlg-name"
              label="Nazwa obszaru"
              required
              value={dlg.name}
              onChange={(e) => setDlg({ ...dlg, name: e.target.value, err: "" })}
              error={dlg.err || undefined}
            />
            <Textarea
              id="dlg-desc"
              label="Opis"
              optional
              rows={3}
              maxLength={1000}
              value={dlg.description}
              onChange={(e) => setDlg({ ...dlg, description: e.target.value })}
            />
            <div className={s.dlgActions}>
              <Button variant="ghost" onClick={() => setDlg(null)}>
                Anuluj
              </Button>
              <Button type="submit" disabled={dlg.saving}>
                Zapisz
              </Button>
            </div>
          </form>
        )}
      </Dialog>
    </div>
  );
}
