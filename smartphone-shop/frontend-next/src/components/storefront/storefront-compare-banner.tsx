"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchCompare, subscribeCompareUpdated } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export function StorefrontCompareBanner() {
  const [compareCount, setCompareCount] = useState(0);
  const [maxCompare, setMaxCompare] = useState(3);

  useEffect(() => {
    let alive = true;

    async function syncCount() {
      try {
        const compare = await fetchCompare();
        if (!alive) {
          return;
        }
        setCompareCount(compare.ids.length);
        setMaxCompare(compare.maxCompare);
      } catch {
        if (!alive) {
          return;
        }
        setCompareCount(0);
      }
    }

    void syncCount();

    const unsubscribe = subscribeCompareUpdated((compare) => {
      setCompareCount(compare.ids.length);
      setMaxCompare(compare.maxCompare);
    });

    const onWindowFocus = () => {
      void syncCount();
    };

    window.addEventListener("focus", onWindowFocus);

    return () => {
      alive = false;
      unsubscribe();
      window.removeEventListener("focus", onWindowFocus);
    };
  }, []);

  if (compareCount <= 0) {
    return null;
  }

  return (
    <div className="pointer-events-none fixed bottom-5 left-1/2 z-[60] w-[min(92vw,760px)] -translate-x-1/2">
      <Link
        href="/compare"
        className="pointer-events-auto flex items-center justify-between gap-3 rounded-2xl border border-zinc-600/60 bg-[#111111]/95 px-4 py-3 text-sm shadow-[0_8px_18px_rgba(0,0,0,0.32)] transition-[transform,border-color] duration-200 hover:-translate-y-px hover:border-zinc-400/80"
      >
        <span className="inline-flex items-center gap-2 text-slate-100">
          <GriddyIcon name="clipboard" />
          Compare list: <strong className="text-zinc-100">{compareCount}</strong> / {maxCompare} products
        </span>
        <span className="inline-flex items-center gap-2 font-semibold text-zinc-100">
          Open compare
          <GriddyIcon name="arrow-right" />
        </span>
      </Link>
    </div>
  );
}
