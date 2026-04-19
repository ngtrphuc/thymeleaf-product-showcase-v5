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
  const [pickerTargetIndex, setPickerTargetIndex] = useState<number | null>(null);
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

  async function onOpenPicker(targetIndex: number) {
    setPickerTargetIndex(targetIndex);
    setPickerOpen(true);
    if (pickerData === null) {
      await loadPickerProducts(pickerKeyword);
    }
  }

  function onClosePicker() {
    setPickerOpen(false);
    setPickerTargetIndex(null);
    setPickerError(null);
  }

  async function onSearchPicker(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadPickerProducts(pickerKeyword);
  }

  async function onSelectProductForSlot(productId: number) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const targetItem =
        pickerTargetIndex !== null && compare?.products ? compare.products[pickerTargetIndex] ?? null : null;

      if (targetItem?.id && targetItem.id !== productId) {
        await removeCompareItem(targetItem.id);
      }

      const data = await addCompareItem(productId);
      setCompare(data);
      setMessage("Product added to compare slot.");
      onClosePicker();
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
  const maxCompare = compare?.maxCompare ?? 3;
  const slots = Array.from({ length: maxCompare }, (_, index) => items[index] ?? null);
  const targetItem = pickerTargetIndex !== null ? slots[pickerTargetIndex] : null;

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Compare Products</h1>
            <p className="mt-2 text-sm text-slate-600">Limit: {maxCompare} products. Add directly at each compare slot.</p>
          </div>
          {items.length > 0 ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => mutate(() => clearCompare())}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
            >
              <GriddyIcon name="close-circle" />
              Clear Compare List
            </button>
          ) : null}
        </div>
      </header>

      {pickerOpen ? (
        <section className="glass-panel space-y-4 rounded-3xl p-4 sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sm text-slate-700">
              {pickerTargetIndex !== null ? `Adding product to slot #${pickerTargetIndex + 1}` : "Select product to compare"}
            </p>
            <button
              type="button"
              onClick={onClosePicker}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-1.5 text-xs"
            >
              <GriddyIcon name="close-circle" />
              Close
            </button>
          </div>

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
                const inCompare = !!product.id && (compare?.ids ?? []).includes(product.id);
                const sameAsTarget = !!product.id && !!targetItem?.id && targetItem.id === product.id;
                const selectable = !!product.id && (!inCompare || sameAsTarget);
                return (
                  <article
                    key={product.id ?? `${product.name}-${product.brand}`}
                    className="rounded-2xl border border-[var(--color-border)] bg-white p-3"
                  >
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
                      disabled={busy || !selectable}
                      onClick={() => {
                        if (product.id) {
                          void onSelectProductForSlot(product.id);
                        }
                      }}
                      className="ui-btn ui-btn-primary mt-3 inline-flex w-full items-center justify-center gap-2 px-3 py-2 text-xs"
                    >
                      <GriddyIcon name={sameAsTarget ? "check" : "clipboard"} />
                      {sameAsTarget
                        ? "Selected in this slot"
                        : inCompare
                          ? "Already in compare"
                          : pickerTargetIndex !== null
                            ? `Add to Slot #${pickerTargetIndex + 1}`
                            : "Add to Compare"}
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

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {slots.map((item, index) =>
          item ? (
            <article key={item.id ?? `${item.name}-${index}`} className="glass-panel rounded-3xl p-4">
              <div className="mb-2 flex items-center justify-between gap-2">
                <p className="text-xs uppercase tracking-[0.12em] text-slate-500">Slot #{index + 1}</p>
                <button
                  type="button"
                  onClick={() => {
                    void onOpenPicker(index);
                  }}
                  className="ui-btn ui-btn-secondary inline-flex items-center gap-1.5 px-2.5 py-1.5 text-xs"
                >
                  <GriddyIcon name="package" />
                  Add
                </button>
              </div>
              <Image
                src={toAssetUrl(item.imageUrl)}
                alt={item.name}
                width={520}
                height={520}
                className="aspect-square w-full rounded-2xl bg-[var(--color-surface-soft)] object-contain p-2"
                unoptimized
              />
              <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.name}</h2>
              <p className="text-sm text-slate-600">
                {item.storage || "N/A"} / {item.ram || "N/A"}
              </p>
              <p className="mt-1 text-xl font-bold text-[var(--color-primary-strong)]">{formatPriceVnd(item.price)}</p>
              <div className="mt-3 flex flex-wrap gap-2">
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
                  <GriddyIcon name="box" />
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
          ) : (
            <article
              key={`slot-${index}`}
              className="glass-panel flex min-h-[420px] flex-col items-center justify-center rounded-3xl border border-dashed border-[var(--color-border-2)] p-4 text-center"
            >
              <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full border border-[var(--color-border-2)] bg-[var(--color-surface-soft)]">
                <GriddyIcon name="package" />
              </div>
              <p className="text-sm font-semibold text-slate-900">Slot #{index + 1} is empty</p>
              <p className="mt-1 max-w-[240px] text-xs text-slate-600">Pick any product from catalog and add it to this compare slot.</p>
              <button
                type="button"
                onClick={() => {
                  void onOpenPicker(index);
                }}
                className="ui-btn ui-btn-primary mt-4 inline-flex items-center gap-2 px-4 py-2 text-sm"
              >
                <GriddyIcon name="package" />
                Add Product
              </button>
            </article>
          )
        )}
      </div>
    </div>
  );
}

