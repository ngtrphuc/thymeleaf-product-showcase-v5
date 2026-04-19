/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  ApiError,
  clearCart,
  decreaseCartItem,
  fetchCart,
  increaseCartItem,
  removeCartItem,
  type CartResponse,
} from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function CartPage() {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function loadCart() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCart();
      setCart(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load cart.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadCart();
  }, []);

  async function mutate(fn: () => Promise<CartResponse>) {
    setBusy(true);
    setError(null);
    try {
      const data = await fn();
      setCart(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Action failed.");
      }
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading cart...</div>;
  }

  if (error && !cart) {
    return <div className="glass-panel rounded-3xl p-8 text-center text-red-700">{error}</div>;
  }

  const items = cart?.items ?? [];

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Your Cart</h1>
        <p className="mt-2 text-sm text-slate-600">Review your selected products before checkout.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {items.length === 0 ? (
        <div className="glass-panel rounded-3xl p-8 text-center">
          <p className="text-slate-700">Your cart is currently empty.</p>
          <Link
            href="/products"
            className="ui-btn ui-btn-primary mt-4 inline-flex items-center gap-2 px-4 py-2 text-sm"
          >
            <GriddyIcon name="package" />
            Browse Products
          </Link>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {items.map((item) => (
              <article key={item.id} className="glass-panel rounded-2xl p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900">{item.name}</p>
                    <p className="text-sm text-slate-600">{formatPriceVnd(item.price)} each</p>
                    <p className="text-xs text-slate-500">{item.availabilityLabel}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => mutate(() => decreaseCartItem(item.id))}
                      className="ui-btn ui-btn-secondary px-3 py-1.5 text-sm"
                    >
                      -
                    </button>
                    <span className="min-w-8 text-center text-sm font-semibold">{item.quantity}</span>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => mutate(() => increaseCartItem(item.id))}
                      className="ui-btn ui-btn-secondary px-3 py-1.5 text-sm"
                    >
                      +
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => mutate(() => removeCartItem(item.id))}
                      className="ui-btn ui-btn-danger ml-2 inline-flex items-center gap-1.5 px-3 py-1.5 text-sm"
                    >
                      <GriddyIcon name="trash" />
                      Remove
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <section className="glass-panel rounded-3xl p-6">
            <p className="text-sm text-slate-600">Items: {cart?.itemCount ?? 0}</p>
            <p className="mt-1 text-2xl font-bold text-slate-900">Total: {formatPriceVnd(cart?.totalAmount)}</p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button
                type="button"
                disabled={busy}
                onClick={() => mutate(() => clearCart())}
                className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
              >
                <GriddyIcon name="close-circle" />
                Clear Cart
              </button>
              <Link
                href="/checkout"
                className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
              >
                <GriddyIcon name="credit-card" />
                Proceed to Checkout
              </Link>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

