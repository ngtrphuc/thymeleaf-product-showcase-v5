"use client";

import type { CSSProperties, MouseEvent } from "react";
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
  initialDirection?: NavigationDirection;
};

const EXIT_DURATION_MS = 240;

type NavigationDirection = "forward" | "backward";

export function CatalogPagedGrid({
  products,
  paginationItems,
  initialDirection = "forward",
}: CatalogPagedGridProps) {
  const router = useRouter();
  const navigationTimerRef = useRef<number | null>(null);
  const [phase, setPhase] = useState<"idle" | "entering" | "exiting">("entering");
  const [direction, setDirection] = useState<NavigationDirection>(initialDirection);
  const [isReducedMotion, setIsReducedMotion] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  const [pendingHref, setPendingHref] = useState<string | null>(null);
  const previousItem = paginationItems[0];
  const nextItem = paginationItems[paginationItems.length - 1];
  const pageItems = paginationItems.slice(1, -1);
  const currentPageIndex = pageItems.findIndex((item) => item.active);
  const reduceCardMotion = products.length > 12;
  const allowCardFlip = !isReducedMotion && products.length <= 9;

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

    const timerId = window.setTimeout(() => setPhase("idle"), 420);
    return () => window.clearTimeout(timerId);
  }, [isReducedMotion, phase]);

  useEffect(() => {
    return () => {
      if (navigationTimerRef.current !== null) {
        window.clearTimeout(navigationTimerRef.current);
      }
    };
  }, []);

  function resolveDirection(item: PaginationItem): NavigationDirection {
    if (item.icon === "arrow-left") {
      return "backward";
    }

    if (item.icon === "arrow-right") {
      return "forward";
    }

    if (typeof window === "undefined") {
      return "forward";
    }

    const targetPage = new URL(item.href, window.location.origin).searchParams.get("page");
    const parsedTargetPage = Number.parseInt(targetPage ?? "", 10);

    if (!Number.isFinite(parsedTargetPage) || currentPageIndex < 0) {
      return "forward";
    }

    return parsedTargetPage > currentPageIndex ? "forward" : "backward";
  }

  function hrefWithDirection(href: string, nextDirection: NavigationDirection) {
    if (typeof window === "undefined") {
      return href;
    }

    const nextUrl = new URL(href, window.location.origin);
    nextUrl.searchParams.set("dir", nextDirection);
    return `${nextUrl.pathname}${nextUrl.search}${nextUrl.hash}`;
  }

  function handleNavigate(event: MouseEvent<HTMLAnchorElement>, item: PaginationItem) {
    if (item.disabled || item.active || pendingHref !== null) {
      event.preventDefault();
      return;
    }

    const nextDirection = resolveDirection(item);
    const nextHref = hrefWithDirection(item.href, nextDirection);
    setDirection(nextDirection);

    if (isReducedMotion) {
      event.preventDefault();
      startTransition(() => router.push(nextHref, { scroll: false }));
      return;
    }

    event.preventDefault();
    setPendingHref(nextHref);
    setPhase("exiting");

    navigationTimerRef.current = window.setTimeout(() => {
      startTransition(() => router.push(nextHref, { scroll: false }));
    }, EXIT_DURATION_MS);
  }

  function renderPaginationLink(item: PaginationItem, edge: boolean) {
    const isDisabled = item.disabled || pendingHref !== null;
    const isActivePage = !edge && Boolean(item.active);
    const className = edge
      ? `ui-btn pagination-rail-edge inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] text-sm transition-[transform,background-color,color,border-color,box-shadow,opacity] duration-200 ${
          isDisabled
            ? "pointer-events-none opacity-45"
            : "hover:border-white/12 hover:bg-white hover:text-black hover:shadow-[0_8px_18px_rgba(0,0,0,0.32)]"
        }`
      : `ui-btn pagination-rail-link inline-flex h-11 min-w-11 items-center justify-center rounded-xl border px-3 py-1.5 text-sm transition-[transform,background-color,color,border-color,box-shadow,opacity] duration-200 ${
          isActivePage
            ? "pointer-events-none cursor-default border-black/80 bg-[var(--color-primary)] font-semibold text-black"
            : "border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] hover:border-white/12 hover:bg-white hover:text-black hover:shadow-[0_8px_18px_rgba(0,0,0,0.32)]"
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
            ? `catalog-motion-shell is-entering is-${direction}`
            : phase === "exiting"
              ? `catalog-motion-shell is-exiting is-${direction}`
              : `catalog-motion-shell is-${direction}`
        }
      >
        <section
          className={`catalog-grid grid grid-cols-2 gap-5 lg:grid-cols-3 ${
            allowCardFlip && phase === "entering"
              ? "is-flip-enter"
              : allowCardFlip && phase === "exiting"
                ? "is-flip-exit"
                : ""
          }`}
        >
          {products.map((product, index) => (
            <div
              key={product.id ?? `${product.name}-${product.brand}`}
              className="catalog-grid-item"
              style={{ "--i": index } as CSSProperties}
            >
              <ProductCard product={product} motionReduced={reduceCardMotion || pendingHref !== null} />
            </div>
          ))}
        </section>
      </div>

      <nav className="glass-panel hidden flex-col items-center gap-2 rounded-2xl p-3 xl:sticky xl:top-24 xl:flex">
        {previousItem ? renderPaginationLink(previousItem, true) : null}
        <div className="max-h-[52vh] overflow-y-auto px-3 py-2">
          <div className="flex flex-col items-center gap-2">
            {pageItems.map((item) => renderPaginationLink(item, false))}
          </div>
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
