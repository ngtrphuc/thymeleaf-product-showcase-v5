/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, clearCompare, fetchCompare, removeCompareItem, toAssetUrl, type CompareResponse } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";

export default function ComparePage() {
  const [compare, setCompare] = useState<CompareResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function loadCompare() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCompare();
      setCompare(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load compare list.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadCompare();
  }, []);

  async function mutate(fn: () => Promise<CompareResponse>) {
    setBusy(true);
    setError(null);
    try {
      const data = await fn();
      setCompare(data);
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
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading compare list...</div>;
  }

  const items = compare?.products ?? [];

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Compare Products</h1>
        <p className="mt-2 text-sm text-slate-600">Limit: {compare?.maxCompare ?? 0} products.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {items.length === 0 ? (
        <div className="glass-panel rounded-3xl p-8 text-center text-slate-700">No products in compare list.</div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {items.map((item) => (
              <article key={item.id ?? item.name} className="glass-panel rounded-3xl p-4">
                <Image
                  src={toAssetUrl(item.imageUrl)}
                  alt={item.name}
                  width={520}
                  height={520}
                  className="aspect-square w-full rounded-2xl object-cover"
                  unoptimized
                />
                <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.name}</h2>
                <p className="text-sm text-slate-600">{item.storage || "N/A"} / {item.ram || "N/A"}</p>
                <p className="mt-1 text-xl font-bold text-[var(--color-primary-strong)]">{formatPriceVnd(item.price)}</p>
                <div className="mt-3 flex gap-2">
                  <Link
                    href={`/products/${item.id ?? ""}`}
                    className="rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-white"
                  >
                    View
                  </Link>
                  {item.id ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => mutate(() => removeCompareItem(item.id ?? 0))}
                      className="rounded-xl bg-red-50 px-4 py-2 text-sm font-semibold text-red-700"
                    >
                      Remove
                    </button>
                  ) : null}
                </div>
              </article>
            ))}
          </div>

          <button
            type="button"
            disabled={busy}
            onClick={() => mutate(() => clearCompare())}
            className="rounded-xl border border-[var(--color-border)] bg-white px-4 py-2 text-sm font-semibold text-slate-800"
          >
            Clear Compare List
          </button>
        </>
      )}
    </div>
  );
}

