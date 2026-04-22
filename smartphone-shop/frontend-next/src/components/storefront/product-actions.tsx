"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { addCartItem, addCompareItem, addWishlistItem, ApiError } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type ProductActionsProps = {
  productId: number;
  isAdmin?: boolean;
  isAuthenticated?: boolean;
  editHref?: string;
  backHref?: string;
  maxQuantity?: number | null;
};

function clampQuantity(nextValue: number, maxQuantity?: number | null): number {
  const upperBound = maxQuantity && maxQuantity > 0 ? maxQuantity : 99;
  return Math.max(1, Math.min(nextValue, upperBound));
}

export function ProductActions({
  productId,
  isAdmin = false,
  isAuthenticated = false,
  editHref = "/admin/products",
  backHref = "/products",
  maxQuantity,
}: ProductActionsProps) {
  const router = useRouter();
  const inversePrimaryButtonClass =
    "border border-white/10 bg-white text-black hover:border-white/12 hover:bg-[var(--color-surface-soft)] hover:text-white";
  const inverseSecondaryButtonClass =
    "border border-[var(--color-border-2)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] hover:border-white/10 hover:bg-white hover:text-black";
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  function handleBack() {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
      return;
    }
    router.push(backHref);
  }

  async function runAction(action: "cart" | "wishlist" | "compare" | "buyNow") {
    setLoadingAction(action);
    setMessage(null);
    setError(null);
    try {
      if (action === "cart") {
        await addCartItem(productId, quantity);
        setMessage(`Added ${quantity} item(s) to cart.`);
      } else if (action === "wishlist") {
        await addWishlistItem(productId);
        setMessage("Added to wishlist.");
      } else if (action === "compare") {
        const compare = await addCompareItem(productId);
        setMessage(`Added to compare list (${compare.ids.length}/${compare.maxCompare}).`);
      } else {
        await addCartItem(productId, quantity);
        router.push("/checkout");
        router.refresh();
        return;
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
      {isAdmin ? (
        <div className="grid gap-2 sm:grid-cols-3">
          <button
            type="button"
            onClick={() => runAction("compare")}
            disabled={loadingAction !== null}
            className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
          >
            <GriddyIcon name="clipboard" />
            {loadingAction === "compare" ? "Adding..." : "Compare"}
          </button>
          <Link
            href={editHref}
            className={`ui-btn ui-btn-primary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inversePrimaryButtonClass}`}
          >
            <GriddyIcon name="box" />
            Edit
          </Link>
          <button
            type="button"
            onClick={handleBack}
            className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
          >
            <GriddyIcon name="arrow-left" />
            Back
          </button>
        </div>
      ) : isAuthenticated ? (
        <div className="space-y-2">
          <div className="grid gap-2 sm:grid-cols-3">
            <div className="glass-panel flex items-center justify-between rounded-xl px-2 py-1.5">
              <button
                type="button"
                onClick={() => setQuantity((prev) => clampQuantity(prev - 1, maxQuantity))}
                disabled={loadingAction !== null}
                className={`ui-btn ui-btn-secondary h-8 w-8 p-0 text-base ${inverseSecondaryButtonClass}`}
                aria-label="Decrease quantity"
              >
                -
              </button>
              <input
                type="number"
                min={1}
                max={maxQuantity ?? 99}
                value={quantity}
                onChange={(event) => {
                  const raw = Number.parseInt(event.target.value || "1", 10);
                  setQuantity(clampQuantity(Number.isNaN(raw) ? 1 : raw, maxQuantity));
                }}
                className="ui-input mx-2 h-8 w-16 px-2 py-1 text-center text-sm"
              />
              <button
                type="button"
                onClick={() => setQuantity((prev) => clampQuantity(prev + 1, maxQuantity))}
                disabled={loadingAction !== null}
                className={`ui-btn ui-btn-secondary h-8 w-8 p-0 text-base ${inverseSecondaryButtonClass}`}
                aria-label="Increase quantity"
              >
                +
              </button>
            </div>

            <button
              type="button"
              onClick={() => runAction("buyNow")}
              disabled={loadingAction !== null}
              className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
            >
              <GriddyIcon name="credit-card" />
              {loadingAction === "buyNow" ? "Processing..." : "Buy Now"}
            </button>

            <button
              type="button"
              onClick={() => runAction("cart")}
              disabled={loadingAction !== null}
              className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
            >
              <GriddyIcon name="cart" />
              {loadingAction === "cart" ? "Adding..." : "Add to Cart"}
            </button>
          </div>

          <div className="grid gap-2 sm:grid-cols-3">
            <button
              type="button"
              onClick={() => runAction("wishlist")}
              disabled={loadingAction !== null}
              className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
            >
              <GriddyIcon name="heart-outline" />
              {loadingAction === "wishlist" ? "Adding..." : "Wishlist"}
            </button>
            <button
              type="button"
              onClick={() => runAction("compare")}
              disabled={loadingAction !== null}
              className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
            >
              <GriddyIcon name="clipboard" />
              {loadingAction === "compare" ? "Adding..." : "Compare"}
            </button>
            <button
              type="button"
              onClick={handleBack}
              className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
            >
              <GriddyIcon name="arrow-left" />
              Back
            </button>
          </div>
        </div>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => runAction("compare")}
            disabled={loadingAction !== null}
            className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
          >
            <GriddyIcon name="clipboard" />
            {loadingAction === "compare" ? "Adding..." : "Compare"}
          </button>
          <button
            type="button"
            onClick={handleBack}
            className={`ui-btn ui-btn-secondary inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm ${inverseSecondaryButtonClass}`}
          >
            <GriddyIcon name="arrow-left" />
            Back
          </button>
        </div>
      )}
      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-600">{error}</p> : null}
    </div>
  );
}
