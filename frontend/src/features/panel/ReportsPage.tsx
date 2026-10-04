import { useState } from "react";
import { api } from "../../api/client";
import type { ReportStatus } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Button, Icon, Select, StatusBadge, Tabs } from "../../components/ds";
import { errorMessage } from "../../lib/errors";
import { formatDate, reportNo } from "../../lib/format";
import { LABELS, REPORT_BADGE, options } from "../../lib/labels";
import { useSession } from "../../session";
import s from "./Panel.module.css";

const STATUSES = Object.keys(LABELS.reportStatus) as ReportStatus[];

export function ReportsPage() {
  const { role, regions, showToast } = useSession();
  const isAdmin = role === "ADMIN";
  const { data, error, loading, setData } = useApi(() => api.reports(), []);
  const [filter, setFilter] = useState<"ALL" | ReportStatus>("ALL");
  const [open, setOpen] = useState<number | null>(null);
  const [status, setStatus] = useState<ReportStatus>("NOWE");
  const [saving, setSaving] = useState(false);

  if (!data) return <PageState loading={loading} error={error} panel />;

  const regionLabel = (c: string) => regions.find((r) => r.code === c)?.label ?? "";
  const list = filter === "ALL" ? data : data.filter((r) => r.status === filter);
  const tabs = [
    { value: "ALL", label: "Wszystkie", count: data.length },
    ...STATUSES.map((k) => ({
      value: k,
      label: LABELS.reportStatus[k],
      count: data.filter((r) => r.status === k).length,
    })),
  ];

  const save = async (id: number) => {
    setSaving(true);
    try {
      const r = await api.setReportStatus(id, status);
      setData(data.map((x) => (x.id === id ? r : x)));
      showToast(`Status raportu zmieniony na „${LABELS.reportStatus[r.status]}”.`, "success");
    } catch (e) {
      showToast(errorMessage(e), "danger");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={s.page}>
      <div className={s.head}>
        <h1 className="h1">Raporty z dopasowań</h1>
        <p className="muted">
          Opisy problemów, które mieszkańcy przesłali do dopasowania.{" "}
          {isAdmin
            ? "Rozwiń raport, żeby zmienić jego status."
            : "Status raportu zmienia administrator."}
        </p>
      </div>
      <Tabs tabs={tabs} value={filter} onChange={(v) => setFilter(v as typeof filter)} />
      <div className="list">
        {list.map((r) => {
          const ex = open === r.id;
          const bodyId = `rep-${r.id}`;
          return (
            <div key={r.id} className={s.repItem}>
              <button
                type="button"
                className={s.repToggle}
                aria-expanded={ex}
                aria-controls={bodyId}
                onClick={() => {
                  setOpen(ex ? null : r.id);
                  setStatus(r.status);
                }}
              >
                <span className={s.repNo}>{reportNo(r.id)}</span>
                <span className={s.rowMain}>
                  <span className={s.strong}>{r.area ? r.area.name : "Bez obszaru"}</span>
                  <span className={s.sub}>
                    {regionLabel(r.region)} · {r.authorName || "Bez podpisu"}
                  </span>
                </span>
                <span className={s.repDate}>{formatDate(r.createdAt)}</span>
                <StatusBadge status={REPORT_BADGE[r.status]}>
                  {LABELS.reportStatus[r.status]}
                </StatusBadge>
                <Icon name={ex ? "chevron-up" : "chevron-down"} size={20} />
              </button>
              {ex && (
                <div id={bodyId} className={s.repBody}>
                  <p className={s.repDesc}>{r.description}</p>
                  <a href={"#/dopasuj/" + r.id} className="icon-link">
                    Zobacz wynik dopasowania
                  </a>
                  {isAdmin ? (
                    <div className={s.inlineForm}>
                      <Select
                        id="rep-status"
                        label="Status raportu"
                        options={options(LABELS.reportStatus)}
                        value={status}
                        onChange={(e) => setStatus(e.target.value as ReportStatus)}
                        style={{ width: 240, maxWidth: "100%" }}
                      />
                      <Button variant="secondary" disabled={saving} onClick={() => save(r.id)}>
                        Zapisz status
                      </Button>
                    </div>
                  ) : (
                    <p className={s.sub}>Status raportu zmienia administrator.</p>
                  )}
                </div>
              )}
            </div>
          );
        })}
        {!list.length && <p className={s.emptyRow}>Brak raportów o tym statusie.</p>}
      </div>
    </div>
  );
}
