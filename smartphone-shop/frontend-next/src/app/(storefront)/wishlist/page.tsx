/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, fetchWishlist, removeWishlistItem, toAssetUrl, type WishlistResponse } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function WishlistPage() {
  const [wishlist, setWishlist] = useState<WishlistResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  async function loadWishlist() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchWishlist();
      setWishlist(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load wishlist.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadWishlist();
  }, []);

  async function removeItem(productId: number) {
    setBusyId(productId);
    setError(null);
    try {
      const data = await removeWishlistItem(productId);
      setWishlist(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to remove wishlist item.");
      }
    } finally {
      setBusyId(null);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading wishlist...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Wishlist</h1>
        <p className="mt-2 text-sm text-slate-600">Products you are tracking for later purchase.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {!wishlist || wishlist.items.length === 0 ? (
        <div className="glass-panel rounded-3xl p-8 text-center text-slate-700">Wishlist is empty.</div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {wishlist.items.map((item) => (
            <article key={item.productId} className="glass-panel rounded-3xl p-4">
              <Image
                src={toAssetUrl(item.imageUrl)}
                alt={item.name}
                width={520}
                height={520}
                sizes="(max-width: 1024px) 100vw, 33vw"
                className="aspect-square w-full rounded-2xl bg-[var(--color-surface-soft)] object-contain p-2"
              />
              <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.name}</h2>
              <p className="mt-1 text-sm text-slate-600">Stock: {item.stock}</p>
              <p className="mt-1 text-xl font-bold text-[var(--color-primary-strong)]">
                {formatPriceVnd(item.price)}
              </p>
              <div className="mt-3 flex gap-2">
                <Link
                  href={`/products/${item.productId}`}
                  className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
                >
                  <GriddyIcon name="eye" />
                  View
                </Link>
                <button
                  type="button"
                  disabled={busyId === item.productId}
                  onClick={() => void removeItem(item.productId)}
                  className="ui-btn ui-btn-danger inline-flex items-center gap-2 px-4 py-2 text-sm"
                >
                  <GriddyIcon name="trash" />
                  Remove
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

