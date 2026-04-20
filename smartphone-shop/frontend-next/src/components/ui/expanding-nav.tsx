"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type ExpandingNavItem = {
  href: string;
  label: string;
  icon: string;
  badge?: number | string;
};

type ExpandingNavProps = {
  items: ExpandingNavItem[];
  ariaLabel: string;
};

function formatBadgeValue(value: number | string): string {
  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      return "";
    }
    if (value > 99) {
      return "99+";
    }
    return `${Math.max(0, Math.trunc(value))}`;
  }
  return value.trim();
}

function isItemActive(pathname: string, href: string): boolean {
  if (href === "/admin") {
    return pathname === "/admin";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function ExpandingNav({ items, ariaLabel }: ExpandingNavProps) {
  const pathname = usePathname();

  return (
    <nav className="ui-expand-nav" aria-label={ariaLabel}>
      {items.map((item) => {
        const active = isItemActive(pathname, item.href);
        const badgeValue = item.badge ? formatBadgeValue(item.badge) : "";
        return (
          <Link
            key={item.href}
            href={item.href}
            className={`ui-expand-link${active ? " is-active" : ""}`}
            aria-current={active ? "page" : undefined}
            title={item.label}
          >
            <GriddyIcon name={item.icon} className="ui-expand-icon" />
            <span className="ui-expand-label">{item.label}</span>
            {badgeValue ? (
              <span className="ui-expand-badge-corner" aria-label={`${item.label} unread: ${badgeValue}`}>
                {badgeValue}
              </span>
            ) : null}
          </Link>
        );
      })}
    </nav>
  );
}
