"use client";

import { useState } from "react";
import { addCompareItem, addWishlistItem, ApiError, removeWishlistItem } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type QuickProductActionsProps = {
  productId: number;
  initiallyWishlisted: boolean;
};

export function QuickProductActions({ productId, initiallyWishlisted }: QuickProductActionsProps) {
  const [wishlisted, setWishlisted] = useState(initiallyWishlisted);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingAction, setLoadingAction] = useState<"wishlist" | "compare" | null>(null);

  async function onWishlistToggle() {
    setLoadingAction("wishlist");
    setMessage(null);
    setError(null);
    try {
      if (wishlisted) {
        await removeWishlistItem(productId);
        setWishlisted(false);
        setMessage("Removed from wishlist.");
      } else {
        await addWishlistItem(productId);
        setWishlisted(true);
        setMessage("Added to wishlist.");
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
    setMessage(null);
    setError(null);
    try {
      await addCompareItem(productId);
      setMessage("Added to compare list.");
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
    <div className="space-y-2">
      <div className="grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={() => void onWishlistToggle()}
          disabled={loadingAction !== null}
          className="ui-btn ui-btn-secondary inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs"
        >
          <GriddyIcon name={wishlisted ? "heart-filled" : "heart-outline"} />
          {loadingAction === "wishlist" ? "Saving..." : wishlisted ? "Wishlisted" : "Add Wishlist"}
        </button>
        <button
          type="button"
          onClick={() => void onCompareAdd()}
          disabled={loadingAction !== null}
          className="ui-btn ui-btn-secondary inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs"
        >
          <GriddyIcon name="clipboard" />
          {loadingAction === "compare" ? "Adding..." : "Compare"}
        </button>
      </div>
      {message ? <p className="text-xs text-emerald-700">{message}</p> : null}
      {error ? <p className="text-xs text-red-600">{error}</p> : null}
    </div>
  );
}
