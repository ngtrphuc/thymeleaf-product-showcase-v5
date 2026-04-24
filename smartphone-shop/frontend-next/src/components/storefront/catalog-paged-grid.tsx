"use client";

import type { CSSProperties, MouseEvent } from "react";
import { startTransition, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ProductSummary } from "@/lib/api";
import { ProductCard } from "@/components/storefront/product-card";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type PaginationItem = {
  href: string;
  label: string;
  active?: boolean;
  disabled?: boolean;
  icon?: "arrow-left" | "arrow-right";
  iconTrailing?: boolean;
};

type CatalogPagedGridProps = {
  products: ProductSummary[];
  paginationItems: PaginationItem[];
};

const EXIT_DURATION_MS = 180;

export function CatalogPagedGrid({ products, paginationItems }: CatalogPagedGridProps) {
  const router = useRouter();
  const navigationTimerRef = useRef<number | null>(null);
  const [phase, setPhase] = useState<"idle" | "entering" | "exiting">("entering");
  const [isReducedMotion, setIsReducedMotion] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  const [pendingHref, setPendingHref] = useState<string | null>(null);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    const syncPreference = () => {
      setIsReducedMotion(mediaQuery.matches);
    };

    mediaQuery.addEventListener("change", syncPreference);
    return () => mediaQuery.removeEventListener("change", syncPreference);
  }, []);

  useEffect(() => {
    if (isReducedMotion || phase !== "entering") {
      return;
    }

    const timerId = window.setTimeout(() => setPhase("idle"), 360);
    return () => window.clearTimeout(timerId);
  }, [isReducedMotion, phase]);

  useEffect(() => {
    return () => {
      if (navigationTimerRef.current !== null) {
        window.clearTimeout(navigationTimerRef.current);
      }
    };
  }, []);

  function handleNavigate(event: MouseEvent<HTMLAnchorElement>, item: PaginationItem) {
    if (item.disabled || item.active || pendingHref !== null) {
      event.preventDefault();
      return;
    }

    if (isReducedMotion) {
      event.preventDefault();
      startTransition(() => router.push(item.href, { scroll: false }));
      return;
    }

    event.preventDefault();
    setPendingHref(item.href);
    setPhase("exiting");

    navigationTimerRef.current = window.setTimeout(() => {
      startTransition(() => router.push(item.href, { scroll: false }));
    }, EXIT_DURATION_MS);
  }

  return (
    <div
      className={
        phase === "entering"
          ? "catalog-flip-stage is-entering"
          : phase === "exiting"
            ? "catalog-flip-stage is-exiting"
            : "catalog-flip-stage"
      }
    >
      <section className="catalog-flip-grid grid grid-cols-2 gap-5 lg:grid-cols-3">
        {products.map((product, index) => (
          <div
            key={product.id ?? `${product.name}-${product.brand}`}
            className="catalog-flip-card"
            style={{ "--i": index } as CSSProperties}
          >
            <ProductCard product={product} />
          </div>
        ))}
      </section>

      <nav className="glass-panel mt-6 flex items-center justify-between rounded-2xl p-4">
        {paginationItems.map((item, index) => {
          const isEdgeButton = index === 0 || index === paginationItems.length - 1;
          const className = isEdgeButton
            ? `ui-btn inline-flex items-center gap-2 px-4 py-2 text-sm ${
                item.disabled || pendingHref !== null
                  ? "pointer-events-none border border-[var(--color-border)] bg-slate-100 text-slate-400"
                  : item.active
                    ? "ui-btn-primary"
                    : "ui-btn-secondary"
              }`
            : `ui-btn px-3 py-1.5 text-sm ${item.active ? "ui-btn-primary" : "ui-btn-secondary"} ${
                pendingHref !== null ? "pointer-events-none opacity-60" : ""
              }`;

          const content = (
            <>
              {item.icon && !item.iconTrailing ? <GriddyIcon name={item.icon} /> : null}
              <span>{item.label}</span>
              {item.icon && item.iconTrailing ? <GriddyIcon name={item.icon} /> : null}
            </>
          );

          if (index === 1) {
            return (
              <div key="catalog-pagination-pages" className="hidden gap-2 sm:flex">
                {paginationItems.slice(1, -1).map((pageItem) => (
                  <Link
                    key={pageItem.label}
                    href={pageItem.href}
                    onClick={(event) => handleNavigate(event, pageItem)}
                    aria-current={pageItem.active ? "page" : undefined}
                    className={`ui-btn px-3 py-1.5 text-sm ${
                      pageItem.active ? "ui-btn-primary" : "ui-btn-secondary"
                    } ${pendingHref !== null ? "pointer-events-none opacity-60" : ""}`}
                  >
                    {pageItem.label}
                  </Link>
                ))}
              </div>
            );
          }

          if (!isEdgeButton) {
            return null;
          }

          return (
            <Link
              key={item.label}
              href={item.href}
              onClick={(event) => handleNavigate(event, item)}
              aria-disabled={item.disabled || pendingHref !== null}
              className={className}
            >
              {content}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
