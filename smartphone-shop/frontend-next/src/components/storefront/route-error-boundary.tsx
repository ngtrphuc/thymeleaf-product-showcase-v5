"use client";

import { useEffect } from "react";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function RouteErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Route rendering failed:", error);
  }, [error]);

  return (
    <div className="glass-panel rounded-3xl p-8 text-center">
      <h2 className="text-lg font-semibold text-red-700">Something went wrong</h2>
      <p className="mt-2 text-sm text-slate-600">
        {error.message || "The page could not be loaded."}
      </p>
      <button
        type="button"
        onClick={reset}
        className="ui-btn ui-btn-primary mt-4 inline-flex items-center gap-2 px-4 py-2 text-sm"
      >
        <GriddyIcon name="arrow-right" />
        Try again
      </button>
    </div>
  );
}
