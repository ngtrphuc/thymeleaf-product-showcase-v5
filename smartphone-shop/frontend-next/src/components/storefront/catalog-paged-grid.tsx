"use client";

import type { MouseEvent } from "react";
import { startTransition, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronDown, ChevronUp } from "lucide-react";
import { useRouter } from "next/navigation";
import type { ProductSummary } from "@/lib/api";
import { ProductCard } from "@/components/storefront/product-card";

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

const EXIT_DURATION_MS = 140;

export function CatalogPagedGrid({ products, paginationItems }: CatalogPagedGridProps) {
  const router = useRouter();
  const navigationTimerRef = useRef<number | null>(null);
  const [phase, setPhase] = useState<"idle" | "entering" | "exiting">("entering");
  const [isReducedMotion, setIsReducedMotion] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  const [pendingHref, setPendingHref] = useState<string | null>(null);
  const previousItem = paginationItems[0];
  const nextItem = paginationItems[paginationItems.length - 1];
  const pageItems = paginationItems.slice(1, -1);
  const reduceCardMotion = products.length >= 8;

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

    const timerId = window.setTimeout(() => setPhase("idle"), 240);
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

  function renderPaginationLink(item: PaginationItem, edge: boolean) {
    const isDisabled = item.disabled || pendingHref !== null;
    const isActivePage = !edge && Boolean(item.active);
    const className = edge
      ? `ui-btn inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] text-sm transition-[transform,background-color,color,border-color,box-shadow,opacity] duration-200 ${
          isDisabled
            ? "pointer-events-none opacity-45"
            : "hover:-translate-y-0.5 hover:border-white/12 hover:bg-white hover:text-black hover:shadow-[0_8px_18px_rgba(0,0,0,0.32)]"
        }`
      : `ui-btn inline-flex h-11 min-w-11 items-center justify-center rounded-xl border px-3 py-1.5 text-sm transition-[transform,background-color,color,border-color,box-shadow,opacity] duration-200 ${
          isActivePage
            ? "pointer-events-none cursor-default border-black/80 bg-[var(--color-primary)] font-semibold text-black hover:!translate-y-0"
            : "border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] hover:-translate-y-0.5 hover:border-white/12 hover:bg-white hover:text-black hover:shadow-[0_8px_18px_rgba(0,0,0,0.32)]"
        } ${isDisabled ? "pointer-events-none opacity-55" : ""}`;

    return (
      <Link
        key={`${item.label}-${item.href}`}
        href={item.href}
        onClick={(event) => handleNavigate(event, item)}
        aria-current={item.active ? "page" : undefined}
        aria-disabled={item.disabled || pendingHref !== null}
        className={className}
      >
        {edge ? (
          <>
            {item.icon === "arrow-left" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            <span className="sr-only">{item.label}</span>
          </>
        ) : (
          <span>{item.label}</span>
        )}
      </Link>
    );
  }

  return (
    <div className="xl:grid xl:grid-cols-[minmax(0,1fr)_auto] xl:items-start xl:gap-4">
      <div
        className={
          phase === "entering"
            ? "catalog-motion-shell is-entering"
            : phase === "exiting"
              ? "catalog-motion-shell is-exiting"
              : "catalog-motion-shell"
        }
      >
        <section className="catalog-grid grid grid-cols-2 gap-5 lg:grid-cols-3">
          {products.map((product) => (
            <div key={product.id ?? `${product.name}-${product.brand}`} className="catalog-grid-item">
              <ProductCard product={product} motionReduced={reduceCardMotion || pendingHref !== null} />
            </div>
          ))}
        </section>
      </div>

      <nav className="glass-panel hidden flex-col items-center gap-2 rounded-2xl p-3 xl:sticky xl:top-24 xl:flex">
        {previousItem ? renderPaginationLink(previousItem, true) : null}
        <div className="flex max-h-[52vh] flex-col gap-2 overflow-y-auto pr-1">
          {pageItems.map((item) => renderPaginationLink(item, false))}
        </div>
        {nextItem ? renderPaginationLink(nextItem, true) : null}
      </nav>

      <nav className="glass-panel mt-6 flex items-center justify-between rounded-2xl p-4 xl:hidden">
        {previousItem ? renderPaginationLink(previousItem, true) : null}
        <div className="mx-3 flex max-w-[55vw] items-center gap-2 overflow-x-auto">
          {pageItems.map((item) => renderPaginationLink(item, false))}
        </div>
        {nextItem ? renderPaginationLink(nextItem, true) : null}
      </nav>
    </div>
  );
}
