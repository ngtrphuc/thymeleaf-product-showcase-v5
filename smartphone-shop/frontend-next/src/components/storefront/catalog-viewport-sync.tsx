"use client";

import { startTransition, useEffect } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

const DESKTOP_BREAKPOINT_PX = 1024;
const DESKTOP_PAGE_SIZE = "9";
const COMPACT_PAGE_SIZE = "8";

type CatalogViewportSyncProps = {
  currentPageSize: number;
};

export function CatalogViewportSync({ currentPageSize }: CatalogViewportSyncProps) {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const searchParamsString = searchParams.toString();
  const requestedPageSize = searchParams.get("pageSize");

  useEffect(() => {
    const mediaQuery = window.matchMedia(`(min-width: ${DESKTOP_BREAKPOINT_PX}px)`);

    function syncPageSize() {
      const desiredPageSize = mediaQuery.matches ? DESKTOP_PAGE_SIZE : COMPACT_PAGE_SIZE;
      const effectivePageSize = requestedPageSize ?? String(currentPageSize);

      if (effectivePageSize === desiredPageSize) {
        return;
      }

      const nextParams = new URLSearchParams(searchParamsString);

      nextParams.set("pageSize", desiredPageSize);
      const nextQuery = nextParams.toString();

      startTransition(() => {
        router.replace(nextQuery ? `${pathname}?${nextQuery}` : pathname, { scroll: false });
      });
    }

    syncPageSize();

    mediaQuery.addEventListener("change", syncPageSize);
    return () => mediaQuery.removeEventListener("change", syncPageSize);
  }, [currentPageSize, pathname, requestedPageSize, router, searchParamsString]);

  return null;
}
