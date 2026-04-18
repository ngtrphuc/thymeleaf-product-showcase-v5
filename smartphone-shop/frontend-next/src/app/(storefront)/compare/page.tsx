/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import {
  addCartItem,
  addCompareItem,
  ApiError,
  clearCompare,
  fetchCatalogPage,
  fetchCompare,
  removeCompareItem,
  toAssetUrl,
  type CatalogPageResponse,
  type CompareResponse,
} from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function ComparePage() {
  const [compare, setCompare] = useState<CompareResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerKeyword, setPickerKeyword] = useState("");
  const [pickerLoading, setPickerLoading] = useState(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [pickerData, setPickerData] = useState<CatalogPageResponse | null>(null);

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
    setMessage(null);
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

  async function onAddToCart(productId: number) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await addCartItem(productId, 1);
      setMessage("Added product to cart.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to add product to cart.");
      }
    } finally {
      setBusy(false);
    }
  }

  async function loadPickerProducts(keyword: string) {
    setPickerLoading(true);
    setPickerError(null);
    try {
      const params = new URLSearchParams();
      params.set("page", "0");
      params.set("sort", "name_asc");
      if (keyword.trim()) {
        params.set("keyword", keyword.trim());
      }
      const data = await fetchCatalogPage(params);
      setPickerData(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setPickerError(err.message);
      } else {
        setPickerError("Failed to load products.");
      }
    } finally {
      setPickerLoading(false);
    }
  }

  async function onOpenPicker() {
    setPickerOpen((prev) => !prev);
    if (!pickerOpen && pickerData === null) {
      await loadPickerProducts(pickerKeyword);
    }
  }

  async function onSearchPicker(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadPickerProducts(pickerKeyword);
  }

  async function onAddToCompare(productId: number) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const data = await addCompareItem(productId);
      setCompare(data);
      setMessage("Product added to compare list.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to add product to compare.");
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
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Compare Products</h1>
            <p className="mt-2 text-sm text-slate-600">Limit: {compare?.maxCompare ?? 0} products.</p>
          </div>
          <button
            type="button"
            onClick={() => {
              void onOpenPicker();
            }}
            className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
          >
            <GriddyIcon name="package" />
            {pickerOpen ? "Close Product List" : "Add Product"}
          </button>
        </div>
      </header>

      {pickerOpen ? (
        <section className="glass-panel space-y-4 rounded-3xl p-4 sm:p-5">
          <form onSubmit={onSearchPicker} className="flex flex-wrap items-center gap-2">
            <input
              value={pickerKeyword}
              onChange={(event) => setPickerKeyword(event.target.value)}
              placeholder="Search product to add..."
              className="ui-input min-w-[220px] flex-1 px-3 py-2 text-sm"
            />
            <button type="submit" className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm">
              <GriddyIcon name="box" />
              Search
            </button>
          </form>

          {pickerError ? <p className="text-sm text-red-700">{pickerError}</p> : null}

          {pickerLoading ? (
            <p className="text-sm text-slate-600">Loading products...</p>
          ) : !pickerData || pickerData.products.length === 0 ? (
            <p className="text-sm text-slate-600">No products found.</p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {pickerData.products.map((product) => {
                const compared = !!product.id && (compare?.ids ?? []).includes(product.id);
                return (
                  <article key={product.id ?? `${product.name}-${product.brand}`} className="rounded-2xl border border-[var(--color-border)] bg-white p-3">
                    <div className="flex items-center gap-3">
                      <Image
                        src={toAssetUrl(product.imageUrl)}
                        alt={product.name}
                        width={88}
                        height={88}
                        className="h-[72px] w-[72px] rounded-xl bg-[var(--color-surface-soft)] object-contain p-1.5"
                        unoptimized
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-slate-900">{product.name}</p>
                        <p className="truncate text-xs text-slate-600">{product.brand}</p>
                        <p className="mt-1 text-sm font-semibold text-[var(--color-primary-strong)]">
                          {formatPriceVnd(product.price)}
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      disabled={busy || compared || !product.id}
                      onClick={() => {
                        if (product.id) {
                          void onAddToCompare(product.id);
                        }
                      }}
                      className="ui-btn ui-btn-primary mt-3 inline-flex w-full items-center justify-center gap-2 px-3 py-2 text-xs"
                    >
                      <GriddyIcon name={compared ? "check" : "clipboard"} />
                      {compared ? "Already Added" : "Add to Compare"}
                    </button>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      ) : null}

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
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
                  className="aspect-square w-full rounded-2xl bg-[var(--color-surface-soft)] object-contain p-2"
                  unoptimized
                />
                <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.name}</h2>
                <p className="text-sm text-slate-600">{item.storage || "N/A"} / {item.ram || "N/A"}</p>
                <p className="mt-1 text-xl font-bold text-[var(--color-primary-strong)]">{formatPriceVnd(item.price)}</p>
                <div className="mt-3 flex gap-2">
                  {item.id ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => void onAddToCart(item.id ?? 0)}
                      className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
                    >
                      <GriddyIcon name="cart" />
                      Add to Cart
                    </button>
                  ) : null}
                  <Link
                    href={`/products/${item.id ?? ""}`}
                    className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
                  >
                    <GriddyIcon name="eye" />
                    View
                  </Link>
                  {item.id ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => mutate(() => removeCompareItem(item.id ?? 0))}
                      className="ui-btn ui-btn-danger inline-flex items-center gap-2 px-4 py-2 text-sm"
                    >
                      <GriddyIcon name="trash" />
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
            className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
          >
            <GriddyIcon name="close-circle" />
            Clear Compare List
          </button>
        </>
      )}
    </div>
  );
}

