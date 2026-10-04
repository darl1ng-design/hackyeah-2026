// Types for the vendored design-system module ds.js (props mirror the DS component signatures).
import type { CSSProperties, ReactNode } from "react";

type Base = { style?: CSSProperties; children?: ReactNode };
type Field = Base & {
  id?: string;
  label?: string;
  hint?: string;
  error?: string;
  optional?: boolean;
  required?: boolean;
  disabled?: boolean;
};
export type Option = { value: string; label: string; hint?: string };
export type Tone =
  | "neutral"
  | "brand"
  | "accent"
  | "inverse"
  | "success"
  | "info"
  | "warning"
  | "danger";
export type ReportBadge =
  | "new"
  | "progress"
  | "forwarded"
  | "resolved"
  | "rejected";

export function Icon(
  p: Base & { name: string; size?: number; color?: string; label?: string },
): ReactNode;
export function Button(
  p: Base & {
    variant?: "primary" | "secondary" | "accent" | "outline" | "ghost";
    size?: "sm" | "md" | "lg";
    iconLeft?: string;
    iconRight?: string;
    fullWidth?: boolean;
    disabled?: boolean;
    type?: "button" | "submit";
    onClick?: () => void;
  },
): ReactNode;
export function IconButton(
  p: Base & {
    icon: string;
    label: string;
    variant?: string;
    size?: "sm" | "md" | "lg";
    disabled?: boolean;
    onClick?: () => void;
  },
): ReactNode;
export function Logo(
  p: Base & {
    variant?: "color" | "reversed";
    size?: number;
    showName?: boolean;
    name?: string;
    tagline?: string;
  },
): ReactNode;
export function Alert(
  p: Base & {
    tone?: "info" | "success" | "warning" | "danger";
    variant?: "card" | "banner";
    title?: string;
    action?: ReactNode;
    onClose?: () => void;
  },
): ReactNode;
export function Badge(
  p: Base & { tone?: Tone; icon?: string; size?: "sm" | "md" },
): ReactNode;
export function StatusBadge(p: Base & { status?: ReportBadge }): ReactNode;
export function Dialog(
  p: Base & {
    open: boolean;
    title?: string;
    footer?: ReactNode;
    onClose?: () => void;
    width?: number;
  },
): ReactNode;
export function Toast(
  p: Base & {
    tone?: "neutral" | "success" | "danger" | "info" | "warning";
    message: string;
    actionLabel?: string;
    onAction?: () => void;
    onClose?: () => void;
  },
): ReactNode;
export function SearchField(
  p: Base & {
    placeholder?: string;
    value?: string;
    defaultValue?: string;
    onChange?: (v: string) => void;
    onSubmit?: (v: string) => void;
    size?: "md" | "lg";
    buttonLabel?: string;
    label?: string;
  },
): ReactNode;
export function RadioGroup(
  p: Base & {
    name: string;
    legend?: string;
    options: Option[];
    value?: string;
    onChange?: (v: string) => void;
    direction?: "row" | "column";
  },
): ReactNode;
export function Select(
  p: Field & {
    options: Option[];
    placeholder?: string;
    value?: string;
    onChange?: (e: { target: { value: string } }) => void;
  },
): ReactNode;
export function Switch(
  p: Base & {
    id?: string;
    label?: string;
    checked?: boolean;
    disabled?: boolean;
    onChange?: (checked: boolean) => void;
  },
): ReactNode;
export function TextField(
  p: Field & {
    type?: string;
    value?: string;
    placeholder?: string;
    iconLeft?: string;
    onChange?: (e: { target: { value: string } }) => void;
    inputStyle?: CSSProperties;
    autoComplete?: string;
  },
): ReactNode;
export function Textarea(
  p: Field & {
    rows?: number;
    maxLength?: number;
    value?: string;
    placeholder?: string;
    onChange?: (e: { target: { value: string } }) => void;
  },
): ReactNode;
export function Breadcrumbs(
  p: Base & {
    items: { label: string; href?: string }[];
    onNavigate?: (item: { label: string; href?: string }, i: number) => void;
  },
): ReactNode;
export function Pagination(
  p: Base & {
    page: number;
    pageCount: number;
    onChange: (page: number) => void;
  },
): ReactNode;
export function Tabs(
  p: Base & {
    tabs: { value: string; label: string; count?: number }[];
    value: string;
    onChange: (v: string) => void;
  },
): ReactNode;
export function Card(
  p: Base & {
    eyebrow?: ReactNode;
    title?: ReactNode;
    icon?: string;
    footer?: ReactNode;
    onClick?: () => void;
    href?: string;
    padding?: number;
    tone?: string;
  },
): ReactNode;
export function Tag(
  p: Base & {
    selected?: boolean;
    onClick?: () => void;
    onRemove?: () => void;
    icon?: string;
  },
): ReactNode;
