"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { addCompareItem, addWishlistItem, ApiError, removeWishlistItem } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type QuickProductActionsProps = {
  productId: number;
  initiallyWishlisted: boolean;
  className?: string;
};

export function QuickProductActions({ productId, initiallyWishlisted, className }: QuickProductActionsProps) {
  const [wishlisted, setWishlisted] = useState(initiallyWishlisted);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingAction, setLoadingAction] = useState<"wishlist" | "compare" | null>(null);
  const [compareAdded, setCompareAdded] = useState(false);
  const [compareCount, setCompareCount] = useState(0);

  useEffect(() => {
    if (!compareAdded) {
      return;
    }

    const timeoutId = window.setTimeout(() => setCompareAdded(false), 1800);
    return () => window.clearTimeout(timeoutId);
  }, [compareAdded]);

  useEffect(() => {
    if (!error) {
      return;
    }

    const timeoutId = window.setTimeout(() => setError(null), 2400);
    return () => window.clearTimeout(timeoutId);
  }, [error]);

  async function onWishlistToggle() {
    setLoadingAction("wishlist");
    setStatus(null);
    setError(null);
    try {
      if (wishlisted) {
        await removeWishlistItem(productId);
        setWishlisted(false);
        setStatus("Removed from wishlist.");
      } else {
        await addWishlistItem(productId);
        setWishlisted(true);
        setStatus("Added to wishlist.");
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Wishlist action failed.");
      }
    } finally {
      setLoadingAction(null);
    }
  }

  async function onCompareAdd() {
    setLoadingAction("compare");
    setStatus(null);
    setError(null);
    try {
      const compare = await addCompareItem(productId);
      setCompareAdded(true);
      setCompareCount(compare.ids.length);
      setStatus(`Added to compare list (${compare.ids.length}/${compare.maxCompare}).`);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Compare action failed.");
      }
    } finally {
      setLoadingAction(null);
    }
  }

  return (
    <div className={`absolute right-3 top-3 z-10 flex flex-col items-end gap-2 ${className ?? ""}`.trim()}>
      <div className="flex items-center gap-2 rounded-full bg-slate-950/12 p-1.5 backdrop-blur-sm">
        <button
          type="button"
          onClick={() => void onWishlistToggle()}
          disabled={loadingAction !== null}
          aria-label={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
          aria-pressed={wishlisted}
          title={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
          className={`inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/16 bg-black/92 text-white shadow-lg transition duration-200 hover:-translate-y-0.5 hover:border-black/10 hover:bg-white hover:text-black disabled:cursor-wait disabled:opacity-70 ${
            wishlisted ? "ring-1 ring-white/35" : ""
          }`}
        >
          <span className="sr-only">
            {loadingAction === "wishlist" ? "Saving wishlist" : wishlisted ? "Wishlisted" : "Add wishlist"}
          </span>
          <GriddyIcon
            name={wishlisted ? "heart-filled" : "heart-outline"}
            className={`h-4 w-4 ${loadingAction === "wishlist" ? "animate-pulse" : ""}`}
          />
        </button>
        <button
          type="button"
          onClick={() => void onCompareAdd()}
          disabled={loadingAction !== null}
          aria-label="Add to compare"
          title="Add to compare"
          className={`inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/16 bg-black/92 text-white shadow-lg transition duration-200 hover:-translate-y-0.5 hover:border-black/10 hover:bg-white hover:text-black disabled:cursor-wait disabled:opacity-70 ${
            compareAdded ? "ring-1 ring-white/35" : ""
          }`}
        >
          <span className="sr-only">{loadingAction === "compare" ? "Adding to compare" : "Add to compare"}</span>
          <GriddyIcon
            name={compareAdded ? "check" : "clipboard"}
            className={`h-4 w-4 ${loadingAction === "compare" ? "animate-pulse" : ""}`}
          />
        </button>
      </div>
      <p aria-live="polite" className="sr-only">
        {error ?? status ?? ""}
      </p>
      {error ? (
        <p className="max-w-[11rem] rounded-2xl bg-red-500/92 px-3 py-1.5 text-right text-[11px] font-medium text-white shadow-lg">
          {error}
        </p>
      ) : null}
      {!error && compareCount > 0 ? (
        <Link
          href="/compare"
          className="max-w-[12.5rem] rounded-2xl bg-white/92 px-3 py-1.5 text-right text-[11px] font-semibold text-slate-800 shadow-lg transition hover:bg-white"
        >
          Compare list: {compareCount} item(s)
        </Link>
      ) : null}
    </div>
  );
}
