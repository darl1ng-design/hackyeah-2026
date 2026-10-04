import { useState } from "react";
import { api } from "../../api/client";
import type { InnovationStatus, ResourceKind } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Alert, Button, Icon, Select, Switch, TextField, Textarea } from "../../components/ds";
import { errorMessage, fieldErrors } from "../../lib/errors";
import { LABELS, options } from "../../lib/labels";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./Panel.module.css";

type InnForm = {
  title: string;
  summary: string;
  description: string;
  targetGroup: string;
  status: InnovationStatus;
  region: string;
  areaId: string;
  videoUrl: string;
  sourceUrl: string;
  published: boolean;
};
type ResForm = { name: string; url: string; kind: ResourceKind; published: boolean };
type Form = InnForm | ResForm;

async function load(isInn: boolean, id: number | null): Promise<Form> {
  if (isInn) {
    if (id === null)
      return { title: "", summary: "", description: "", targetGroup: "", status: "ROZWOJ", region: "MALOPOLSKA", areaId: "", videoUrl: "", sourceUrl: "", published: false };
    const i = await api.adminInnovation(id);
    return {
      title: i.title,
      summary: i.summary || "",
      description: i.description || "",
      targetGroup: i.targetGroup || "",
      status: i.status,
      region: i.region || "",
      areaId: i.area ? String(i.area.id) : "",
      videoUrl: i.videoUrl || "",
      sourceUrl: i.sourceUrl || "",
      published: !!i.published,
    };
  }
  if (id === null) return { name: "", url: "", kind: "BIBLIOTEKA", published: false };
  const x = await api.adminResource(id);
  return { name: x.name, url: x.url, kind: x.kind, published: !!x.published };
}

export function EditorPage({ route }: ScreenProps) {
  const isInn = route.params.type === "innowacja";
  const id = route.params.id === "nowy" ? null : +route.params.id;
  const { data, error, loading } = useApi(() => load(isInn, id), [isInn, id]);
  if (!data) return <PageState loading={loading} error={error} panel />;
  return <EditorForm isInn={isInn} id={id} initial={data} />;
}

function EditorForm({ isInn, id, initial }: { isInn: boolean; id: number | null; initial: Form }) {
  const { areas, regions, reloadAreas, showToast } = useSession();
  const [form, setForm] = useState<Record<string, string | boolean>>(initial);
  const [err, setErr] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const isNew = id === null;
  const back = "/panel/wiedza?tab=" + (isInn ? "inn" : "res");

  const str = (k: string) => String(form[k] ?? "");
  const bind = (k: string) => ({
    value: str(k),
    error: err[k] || undefined,
    onChange: (e: { target: { value: string } }) => {
      setForm((f) => ({ ...f, [k]: e.target.value }));
      setErr((x) => ({ ...x, [k]: "" }));
    },
  });

  const save = async () => {
    setSaving(true);
    setErr({});
    try {
      if (isInn) {
        const f = form as InnForm;
        await api.saveInnovation(id, { ...f, areaId: f.areaId ? +f.areaId : null });
        reloadAreas();
      } else {
        await api.saveResource(id, form as ResForm);
      }
      showToast(isNew ? "Wpis dodany." : "Zmiany zapisane.", "success");
      go(back);
    } catch (e) {
      const f = fieldErrors(e);
      setErr(f);
      setSaving(false);
      if (!Object.keys(f).length) showToast(errorMessage(e), "danger");
    }
  };

  const errors = Object.values(err).filter(Boolean);

  return (
    <form
      className={`${s.page} ${s.narrow}`}
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <a href={"#" + back} className="icon-link">
        <Icon name="arrow-left" size={16} />
        Wróć do bazy wiedzy
      </a>
      <h1 className="h1">
        {isNew ? (isInn ? "Nowa innowacja" : "Nowy zasób") : isInn ? "Edycja innowacji" : "Edycja zasobu"}
      </h1>
      {errors.length > 0 && (
        <div role="alert">
          <Alert tone="danger" title="Popraw zaznaczone pola">
            {errors.join(" ")}
          </Alert>
        </div>
      )}
      <div className={`${s.card} ${s.formCard}`}>
        {isInn ? (
          <>
            <TextField id="ed-title" label="Tytuł" required {...bind("title")} />
            <Textarea
              id="ed-summary"
              label="Zajawka"
              optional
              hint="Jedno–dwa zdania widoczne na karcie w katalogu."
              rows={3}
              maxLength={1000}
              {...bind("summary")}
            />
            <Textarea id="ed-desc" label="Pełny opis" optional rows={8} {...bind("description")} />
            <TextField id="ed-target" label="Grupa docelowa" optional {...bind("targetGroup")} />
            <div className={s.grid3}>
              <Select id="ed-status" label="Status" options={options(LABELS.innovationStatus)} {...bind("status")} />
              <Select
                id="ed-region"
                label="Region"
                options={regions.map((r) => ({ value: r.code, label: r.label }))}
                placeholder="Wybierz region"
                {...bind("region")}
              />
              <Select
                id="ed-area"
                label="Obszar"
                options={areas.map((a) => ({ value: String(a.id), label: a.name }))}
                placeholder="Wybierz obszar"
                {...bind("areaId")}
              />
            </div>
            <TextField
              id="ed-video"
              label="Adres wideo"
              optional
              type="url"
              hint="Pełny adres zaczynający się od https://"
              {...bind("videoUrl")}
            />
            <TextField
              id="ed-source"
              label="Adres źródła"
              optional
              type="url"
              hint="Strona organizacji lub opis projektu."
              {...bind("sourceUrl")}
            />
          </>
        ) : (
          <>
            <TextField id="ed-name" label="Nazwa" required {...bind("name")} />
            <TextField
              id="ed-url"
              label="Adres"
              required
              type="url"
              hint="Pełny adres zaczynający się od https://"
              {...bind("url")}
            />
            <Select id="ed-kind" label="Rodzaj" options={options(LABELS.resourceKind)} {...bind("kind")} />
          </>
        )}
        <Switch
          id="ed-pub"
          label="Opublikowany — widoczny w serwisie"
          checked={!!form.published}
          onChange={(v) => setForm((f) => ({ ...f, published: v }))}
        />
      </div>
      <div className="row">
        <Button type="submit" size="lg" disabled={saving}>
          {saving ? "Zapisujemy…" : isNew ? "Dodaj wpis" : "Zapisz zmiany"}
        </Button>
        <Button variant="ghost" size="lg" onClick={() => go(back)}>
          Anuluj
        </Button>
      </div>
    </form>
  );
}
