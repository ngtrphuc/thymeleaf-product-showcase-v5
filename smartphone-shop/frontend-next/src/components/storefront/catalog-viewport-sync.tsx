"use client";

import { startTransition, useEffect } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

const DESKTOP_BREAKPOINT_PX = 1024;
const DESKTOP_PAGE_SIZE = "9";
const COMPACT_PAGE_SIZE = "8";

export function CatalogViewportSync() {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const searchParamsString = searchParams.toString();

  useEffect(() => {
    const mediaQuery = window.matchMedia(`(min-width: ${DESKTOP_BREAKPOINT_PX}px)`);

    function syncPageSize() {
      const desiredPageSize = mediaQuery.matches ? DESKTOP_PAGE_SIZE : COMPACT_PAGE_SIZE;
      const nextParams = new URLSearchParams(searchParamsString);

      if (nextParams.get("pageSize") === desiredPageSize) {
        return;
      }

      nextParams.set("pageSize", desiredPageSize);
      const nextQuery = nextParams.toString();

      startTransition(() => {
        router.replace(nextQuery ? `${pathname}?${nextQuery}` : pathname, { scroll: false });
      });
    }

    syncPageSize();

    if (typeof mediaQuery.addEventListener === "function") {
      mediaQuery.addEventListener("change", syncPageSize);
      return () => mediaQuery.removeEventListener("change", syncPageSize);
    }

    mediaQuery.addListener(syncPageSize);
    return () => mediaQuery.removeListener(syncPageSize);
  }, [pathname, router, searchParamsString]);

  return null;
}
