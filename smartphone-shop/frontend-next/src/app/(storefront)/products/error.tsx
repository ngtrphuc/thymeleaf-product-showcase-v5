"use client";

import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function ProductsError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="glass-panel rounded-3xl p-8 text-center">
      <h2 className="text-lg font-semibold text-red-700">Failed to load products</h2>
      <p className="mt-2 text-sm text-slate-600">{error.message}</p>
      <button
        type="button"
        onClick={reset}
        className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-black transition-[transform,box-shadow,opacity] duration-200 hover:-translate-y-px hover:shadow-[0_10px_22px_rgba(255,255,255,0.18)] active:translate-y-0 active:opacity-90"
      >
        <GriddyIcon name="arrow-right" />
        Try again
      </button>
    </div>
  );
}
