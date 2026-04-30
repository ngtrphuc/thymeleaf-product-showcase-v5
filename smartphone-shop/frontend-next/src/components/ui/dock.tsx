"use client";

import { type ReactNode } from "react";

type DockProps = {
  children: ReactNode;
  className?: string;
};

type DockItemProps = {
  children: ReactNode;
  onClick?: () => void;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
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
  return <nav className={`ui-expand-nav ${className ?? ""}`}>{children}</nav>;
}

export function DockItem({
  children,
  onClick,
  onMouseEnter,
  onMouseLeave,
  ariaLabel,
  className,
  active = false,
  activeLabel,
}: DockItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      aria-label={ariaLabel}
      className={[
        "ui-expand-link ui-header-contrast ui-negative-hover",
        active ? "is-active" : "",
        className ?? "",
      ].join(" ")}
    >
      {children}
      {activeLabel ? (
        <span className="ui-expand-label max-w-[7rem] truncate text-xs">
          {activeLabel}
        </span>
      ) : null}
    </button>
  );
}

export function DockIcon({ children, className }: DockPartProps) {
  return <span className={`ui-expand-icon pointer-events-none flex items-center justify-center ${className ?? ""}`}>{children}</span>;
}
