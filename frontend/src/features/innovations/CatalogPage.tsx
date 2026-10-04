import { useState } from "react";
import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import {
  Breadcrumbs,
  Button,
  Icon,
  Pagination,
  SearchField,
  Select,
  Tag,
} from "../../components/ds";
import { PageState } from "../../components/PageState";
import { plural } from "../../lib/format";
import { go, setQuery } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import { InnovationCard } from "./InnovationCard";
import s from "./CatalogPage.module.css";

const PAGE_SIZE = 6;

export function CatalogPage({ route }: ScreenProps) {
  const { areas, regions } = useSession();
  const { q = "", areaId = "", region = "" } = route.query;
  const page = Math.max(1, Number(route.query.page) || 1);
  const [text, setText] = useState(q);
  const { data, error, loading } = useApi(
    () =>
      api.innovations({
        q,
        areaId: areaId ? Number(areaId) : undefined,
        region,
        page: page - 1,
        size: PAGE_SIZE,
      }),
    [q, areaId, region, page],
  );
  const nav = (patch: Record<string, string | number | undefined>) =>
    setQuery(route, { ...patch, page: undefined });
  const clear = () => go("/innowacje");
  const hasFilters = !!(q || areaId || region);

  return (
    <div className="container stack">
      <div
        className="stack"
        style={{ "--gap": "var(--space-2)" } as React.CSSProperties}
      >
        <Breadcrumbs
          items={[
            { label: "Start", href: "#/" },
            { label: "Katalog innowacji" },
          ]}
        />
        <h1 className="h1">Katalog innowacji</h1>
        <p role="status" className="muted">
          {data &&
            `${data.totalElements} ${plural(data.totalElements, "innowacja", "innowacje", "innowacji")}` +
              (q ? ` dla „${q}”` : "")}
        </p>
      </div>

      <div className={s.search}>
        <SearchField
          size="md"
          label="Szukaj w katalogu"
          placeholder="Wpisz frazę, np. seniorzy"
          buttonLabel="Szukaj"
          value={text}
          onChange={setText}
          onSubmit={(v) => nav({ q: v.trim() })}
        />
      </div>

      <div className="stack" style={{ "--gap": "10px" } as React.CSSProperties}>
        <span className="overline" id="cat-area-label">
          Obszar
        </span>
        <div className="row" role="group" aria-labelledby="cat-area-label">
          <Tag selected={!areaId} onClick={() => nav({ areaId: undefined })}>
            Wszystkie
          </Tag>
          {areas.map((a) => (
            <Tag
              key={a.id}
              selected={String(a.id) === areaId}
              onClick={() => nav({ areaId: a.id })}
            >
              {a.name}
            </Tag>
          ))}
        </div>
      </div>

      <div className={s.filters}>
        <Select
          id="cat-region"
          label="Region"
          options={[
            { value: "", label: "Wszystkie regiony" },
            ...regions.map((r) => ({ value: r.code, label: r.label })),
          ]}
          value={region}
          onChange={(e) => nav({ region: e.target.value })}
          style={{ minWidth: 240, maxWidth: 320 }}
        />
        {hasFilters && (
          <Button variant="ghost" iconLeft="x" onClick={clear}>
            Wyczyść filtry
          </Button>
        )}
      </div>

      {!data ? (
        <PageState loading={loading} error={error} />
      ) : data.content.length ? (
        <>
          <div className="grid">
            {data.content.map((i) => (
              <InnovationCard key={i.id} innovation={i} />
            ))}
          </div>
          {data.totalPages > 1 && (
            <Pagination
              page={page}
              pageCount={data.totalPages}
              onChange={(p) => setQuery(route, { page: p })}
            />
          )}
        </>
      ) : (
        <div className="empty">
          <Icon name="search-x" size={40} color="var(--gray-600)" />
          <h2 className="h3">Nie znaleźliśmy innowacji dla tych filtrów</h2>
          <p className="muted narrow">
            Spróbuj innej frazy lub wyczyść filtry. Możesz też opisać problem
            własnymi słowami — dopasujemy rozwiązania za Ciebie.
          </p>
          <div className="row">
            <Button variant="outline" onClick={clear}>
              Wyczyść filtry
            </Button>
            <Button iconRight="arrow-right" onClick={() => go("/dopasuj")}>
              Opisz problem, dopasujemy
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
