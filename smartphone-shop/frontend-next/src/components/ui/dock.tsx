"use client";

import { type ReactNode } from "react";

type DockProps = {
  children: ReactNode;
  className?: string;
};

type DockItemProps = {
  children: ReactNode;
  onClick?: () => void;
  ariaLabel?: string;
  className?: string;
  active?: boolean;
  activeLabel?: ReactNode;
};

type DockPartProps = {
  children: ReactNode;
  className?: string;
};

export function Dock({ children, className }: DockProps) {
  return (
    <div className={`flex h-24 items-end justify-center ${className ?? ""}`}>
      <div className="flex items-end gap-3 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 shadow-[0_10px_34px_rgba(0,0,0,0.45)]">
        {children}
      </div>
    </div>
  );
}

export function DockItem({ children, onClick, ariaLabel, className, active = false, activeLabel }: DockItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className={[
        "group/dockitem relative flex h-11 origin-bottom items-center rounded-xl border transition-[transform,background-color,color,border-color,box-shadow,width,padding,gap] duration-200 ease-out",
        active
          ? "w-auto min-w-[8rem] justify-start gap-2 border border-black/80 bg-[var(--color-primary)] px-3.5 text-black shadow-[0_10px_22px_rgba(0,0,0,0.38),inset_0_0_0_0.55px_rgba(0,0,0,0.38)]"
          : "w-11 justify-center border-[var(--color-border)] bg-[var(--color-surface-soft)] px-0 text-[var(--color-text-muted)] hover:-translate-y-1 hover:border-white/10 hover:bg-white hover:text-black hover:shadow-[0_10px_22px_rgba(0,0,0,0.38)]",
        className ?? "",
      ].join(" ")}
    >
      {children}
      {active && activeLabel ? (
        <span className="pointer-events-none max-w-[5.9rem] truncate text-xs font-bold tracking-[0.01em]">
          {activeLabel}
        </span>
      ) : null}
    </button>
  );
}

export function DockIcon({ children, className }: DockPartProps) {
  return <span className={`pointer-events-none flex h-5 w-5 items-center justify-center ${className ?? ""}`}>{children}</span>;
}

export function DockLabel({ children, className }: DockPartProps) {
  return (
    <span
      className={`pointer-events-none absolute -top-9 left-1/2 -translate-x-1/2 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] font-medium text-[var(--color-text)] opacity-0 shadow-[0_4px_18px_rgba(0,0,0,0.35)] transition-opacity duration-150 group-hover/dockitem:opacity-100 ${className ?? ""}`}
    >
      {children}
    </span>
  );
}
