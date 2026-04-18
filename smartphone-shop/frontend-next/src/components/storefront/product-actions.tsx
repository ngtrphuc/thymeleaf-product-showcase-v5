"use client";

import { useState } from "react";
import { addCartItem, addCompareItem, addWishlistItem, ApiError } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type ProductActionsProps = {
  productId: number;
};

export function ProductActions({ productId }: ProductActionsProps) {
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  async function runAction(action: "cart" | "wishlist" | "compare") {
    setLoadingAction(action);
    setMessage(null);
    setError(null);
    try {
      if (action === "cart") {
        await addCartItem(productId, 1);
        setMessage("Added to cart.");
      } else if (action === "wishlist") {
        await addWishlistItem(productId);
        setMessage("Added to wishlist.");
      } else {
        await addCompareItem(productId);
        setMessage("Added to compare list.");
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Request failed.");
      }
    } finally {
      setLoadingAction(null);
    }
  }

  return (
    <div className="space-y-3">
      <div className="grid gap-2 sm:grid-cols-3">
        <button
          type="button"
          onClick={() => runAction("cart")}
          disabled={loadingAction !== null}
          className="ui-btn ui-btn-primary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="cart" />
          {loadingAction === "cart" ? "Adding..." : "Add to Cart"}
        </button>
        <button
          type="button"
          onClick={() => runAction("wishlist")}
          disabled={loadingAction !== null}
          className="ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="heart-outline" />
          {loadingAction === "wishlist" ? "Adding..." : "Wishlist"}
        </button>
        <button
          type="button"
          onClick={() => runAction("compare")}
          disabled={loadingAction !== null}
          className="ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="clipboard" />
          {loadingAction === "compare" ? "Adding..." : "Compare"}
        </button>
      </div>
      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-600">{error}</p> : null}
    </div>
  );
}
