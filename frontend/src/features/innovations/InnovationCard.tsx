// Catalog card for one innovation (prototype `innCard`): area eyebrow, title, summary, "region · status" footer.
import type { ReactNode } from "react";
import type { Innovation } from "../../api/types";
import { Card } from "../../components/ds";
import { LABELS } from "../../lib/labels";
import { useSession } from "../../session";

type Props = {
  innovation: Innovation;
  /** Link target; defaults to the detail page. Ignored when onClick is given. */
  href?: string;
  onClick?: () => void;
  /** Overrides the area eyebrow (e.g. similarity label on match results). */
  eyebrow?: ReactNode;
  /** Extra content under the summary. */
  children?: ReactNode;
};

export function InnovationCard({
  innovation: i,
  href,
  onClick,
  eyebrow,
  children,
}: Props) {
  const { regions } = useSession();
  const region = regions.find((r) => r.code === i.region)?.label || "";
  return (
    <Card
      eyebrow={eyebrow ?? i.area?.name ?? ""}
      title={i.title}
      footer={`${region} · ${LABELS.innovationStatus[i.status] || ""}`}
      href={onClick ? undefined : (href ?? `#/innowacje/${i.id}`)}
      onClick={onClick}
    >
      {i.summary}
      {children}
    </Card>
  );
}
