/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  addCartItem,
  ApiError,
  clearCompare,
  fetchCatalogPage,
  fetchCompare,
  removeCompareItem,
  replaceCompareItems,
  toAssetUrl,
  type CatalogPageResponse,
  type CompareResponse,
  type ProductSummary,
} from "@/lib/api";
import { FilterDropdown, type FilterDropdownOption } from "@/components/storefront/filter-dropdown";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

const COMPARE_SLOT_STORAGE_KEY = "storefront-compare-slot-order";
const PICKER_PAGE_SIZE = 6;

type CompareRow = {
  label: string;
  values: string[];
  different: boolean;
};

function createEmptySlotIds(maxCompare: number): Array<number | null> {
  return Array.from({ length: maxCompare }, () => null);
}

function normalizeSlotIds(input: unknown, maxCompare: number): Array<number | null> {
  if (!Array.isArray(input)) {
    return createEmptySlotIds(maxCompare);
  }

  return Array.from({ length: maxCompare }, (_, index) => {
    const value = input[index];
    return typeof value === "number" && Number.isFinite(value) ? value : null;
  });
}

function readStoredSlotIds(maxCompare: number): Array<number | null> {
  if (typeof window === "undefined") {
    return createEmptySlotIds(maxCompare);
  }

  try {
    const raw = window.localStorage.getItem(COMPARE_SLOT_STORAGE_KEY);
    if (!raw) {
      return createEmptySlotIds(maxCompare);
    }
    return normalizeSlotIds(JSON.parse(raw), maxCompare);
  } catch {
    return createEmptySlotIds(maxCompare);
  }
}

function writeStoredSlotIds(slotIds: Array<number | null>) {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.setItem(COMPARE_SLOT_STORAGE_KEY, JSON.stringify(slotIds));
  } catch {
    // Ignore storage write errors (private mode, storage quota, etc.).
  }
}

function reconcileSlotIds(
  preferredSlotIds: Array<number | null>,
  compare: CompareResponse,
): Array<number | null> {
  const next = createEmptySlotIds(compare.maxCompare);
  const compareIds = new Set(compare.ids);
  const used = new Set<number>();

  preferredSlotIds.forEach((id, index) => {
    if (index >= compare.maxCompare || id === null || !compareIds.has(id) || used.has(id)) {
      return;
    }
    next[index] = id;
    used.add(id);
  });

  compare.ids.forEach((id) => {
    if (used.has(id)) {
      return;
    }
    const emptyIndex = next.findIndex((value) => value === null);
    if (emptyIndex !== -1) {
      next[emptyIndex] = id;
      used.add(id);
    }
  });

  return next;
}

function buildSlots(
  maxCompare: number,
  slotProductIds: Array<number | null>,
  items: ProductSummary[],
): Array<ProductSummary | null> {
  const productsById = new Map(items.map((product) => [product.id, product] as const));
  return Array.from({ length: maxCompare }, (_, index) => {
    const productId = slotProductIds[index] ?? null;
    return productId !== null ? productsById.get(productId) ?? null : null;
  });
}

function normalizeCompareCell(value: string): string {
  return value.trim().toLowerCase();
}

function createCompareRow(label: string, values: string[]): CompareRow {
  const comparableValues = values
    .map((value) => value.trim())
    .filter((value) => value.length > 0 && value !== "N/A");
  const different = new Set(comparableValues.map(normalizeCompareCell)).size > 1;
  return { label, values, different };
}

function buildComparisonRows(slots: Array<ProductSummary | null>): CompareRow[] {
  return [
    createCompareRow("Price", slots.map((product) => formatPriceVnd(product?.price ?? null))),
    createCompareRow("Availability", slots.map((product) => product?.availabilityLabel ?? "N/A")),
    createCompareRow("Storage", slots.map((product) => product?.storage || "N/A")),
    createCompareRow("RAM", slots.map((product) => product?.ram || "N/A")),
    createCompareRow("Screen", slots.map((product) => product?.size || "N/A")),
    createCompareRow("Resolution", slots.map((product) => product?.resolution || "N/A")),
    createCompareRow("Operating System", slots.map((product) => product?.os || "N/A")),
    createCompareRow("Chipset", slots.map((product) => product?.chipset || "N/A")),
    createCompareRow("CPU Speed", slots.map((product) => product?.speed || "N/A")),
    createCompareRow("Battery", slots.map((product) => product?.battery || "N/A")),
    createCompareRow("Charging", slots.map((product) => product?.charging || "N/A")),
  ];
}

export default function ComparePage() {
  const [compare, setCompare] = useState<CompareResponse | null>(null);
  const [slotProductIds, setSlotProductIds] = useState<Array<number | null>>(() => readStoredSlotIds(3));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerTargetIndex, setPickerTargetIndex] = useState<number | null>(null);
  const [pickerKeyword, setPickerKeyword] = useState("");
  const [pickerBrand, setPickerBrand] = useState("");
  const [pickerPage, setPickerPage] = useState(0);
  const [pickerLoading, setPickerLoading] = useState(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [pickerData, setPickerData] = useState<CatalogPageResponse | null>(null);
  const [pickerMotionPhase, setPickerMotionPhase] = useState<"idle" | "entering">("idle");
  const [pickerReducedMotion, setPickerReducedMotion] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );

  const applyCompareState = useCallback((data: CompareResponse, preferredSlotIds?: Array<number | null>) => {
    setCompare(data);
    setSlotProductIds((currentSlotIds) => {
      const nextSlotIds = reconcileSlotIds(
        preferredSlotIds ?? normalizeSlotIds(currentSlotIds, data.maxCompare),
        data,
      );
      writeStoredSlotIds(nextSlotIds);
      return nextSlotIds;
    });
  }, []);

  const loadCompare = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCompare();
      applyCompareState(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load compare list.");
      }
    } finally {
      setLoading(false);
    }
  }, [applyCompareState]);

  useEffect(() => {
    void loadCompare();
  }, [loadCompare]);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    const syncPreference = () => {
      setPickerReducedMotion(mediaQuery.matches);
    };

    mediaQuery.addEventListener("change", syncPreference);
    return () => mediaQuery.removeEventListener("change", syncPreference);
  }, []);

  async function mutate(
    fn: () => Promise<CompareResponse>,
    preferredSlotIds: Array<number | null> = slotProductIds,
  ) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const data = await fn();
      applyCompareState(data, preferredSlotIds);
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

  async function loadPickerProducts(
    overrides?: Partial<{
      keyword: string;
      brand: string;
      page: number;
    }>,
  ) {
    const keyword = overrides?.keyword ?? pickerKeyword;
    const brand = overrides?.brand ?? pickerBrand;
    const page = overrides?.page ?? pickerPage;

    setPickerLoading(true);
    setPickerError(null);
    setPickerData(null);
    try {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("pageSize", String(PICKER_PAGE_SIZE));
      params.set("sort", "name_asc");
      if (keyword.trim()) {
        params.set("keyword", keyword.trim());
      }
      if (brand.trim()) {
        params.set("brand", brand.trim());
      }
      const data = await fetchCatalogPage(params);
      setPickerData(data);
      setPickerPage(page);
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
    await loadPickerProducts({ page: 0 });
  }

  function onClosePicker() {
    setPickerOpen(false);
    setPickerTargetIndex(null);
    setPickerError(null);
  }

  async function onSearchPicker(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadPickerProducts({ page: 0 });
  }

  async function onClearPickerFilters() {
    setPickerKeyword("");
    setPickerBrand("");
    await loadPickerProducts({
      keyword: "",
      brand: "",
      page: 0,
    });
  }

  async function onChangePickerPage(nextPage: number) {
    await loadPickerProducts({ page: nextPage });
  }

  async function onSelectProductForSlot(productId: number) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const maxCompare = compare?.maxCompare ?? 3;
      if (pickerTargetIndex === null) {
        return;
      }

      const nextSlotIds = normalizeSlotIds(slotProductIds, maxCompare).map((id) => (id === productId ? null : id));
      nextSlotIds[pickerTargetIndex] = productId;

      const orderedIds = nextSlotIds.filter((id): id is number => id !== null);
      const data = await replaceCompareItems(orderedIds);

      applyCompareState(data, nextSlotIds);
      setMessage(`Product added to Product ${pickerTargetIndex + 1}.`);
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

  const items = compare?.products ?? [];
  const maxCompare = compare?.maxCompare ?? 3;
  const normalizedSlotIds = normalizeSlotIds(slotProductIds, maxCompare);
  const slots = buildSlots(maxCompare, normalizedSlotIds, items);
  const comparisonRows = buildComparisonRows(slots);
  const targetItem = pickerTargetIndex !== null ? slots[pickerTargetIndex] : null;
  const isPickerVisible = pickerOpen && pickerTargetIndex !== null;
  const pickerAnimationKey = useMemo(
    () =>
      isPickerVisible
        ? `${pickerData?.currentPage ?? 0}:${pickerData?.products.map((product) => product.id ?? product.name).join("|") ?? ""}`
        : "",
    [isPickerVisible, pickerData],
  );
  const pickerBrandOptions: FilterDropdownOption[] = [
    { label: "All brands", value: "" },
    ...((pickerData?.brands ?? []).map((brand) => ({ label: brand, value: brand })) as FilterDropdownOption[]),
  ];

  useEffect(() => {
    if (!isPickerVisible || pickerReducedMotion || !pickerData || pickerData.products.length === 0) {
      setPickerMotionPhase("idle");
      return;
    }

    setPickerMotionPhase("entering");
    const timerId = window.setTimeout(() => {
      setPickerMotionPhase("idle");
    }, 760);

    return () => window.clearTimeout(timerId);
  }, [isPickerVisible, pickerAnimationKey, pickerData, pickerReducedMotion]);

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading compare list...</div>;
  }

  function renderPickerPanel(slotIndex: number) {
    if (!isPickerVisible || pickerTargetIndex !== slotIndex) {
      return null;
    }

    return (
      <div className="absolute inset-3 z-10 flex flex-col rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)]/95 p-3 shadow-[0_18px_36px_rgba(0,0,0,0.45)] backdrop-blur-sm">
        <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
          <div>
            <p className="text-sm font-semibold text-slate-900">{`Product ${slotIndex + 1}`}</p>
            <p className="text-xs text-slate-600">Choose a product for this compare position.</p>
          </div>
          <button
            type="button"
            onClick={onClosePicker}
            className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-1.5 text-xs"
          >
            <GriddyIcon name="close-circle" />
            Close
          </button>
        </div>

        <form onSubmit={onSearchPicker} className="mt-3 grid gap-2 sm:grid-cols-2">
          <input
            value={pickerKeyword}
            onChange={(event) => setPickerKeyword(event.target.value)}
            placeholder="Search product..."
            className="ui-input h-12 w-full min-w-0 px-3 text-center text-sm placeholder:text-center"
          />
          <FilterDropdown
            options={pickerBrandOptions}
            value={pickerBrand}
            onChange={setPickerBrand}
            triggerClassName="h-12 justify-between text-center"
          />
          <button
            type="submit"
            className="ui-btn ui-btn-secondary inline-flex h-12 w-full items-center justify-center gap-2 px-4 text-center text-sm"
          >
            <GriddyIcon name="check" />
            Apply
          </button>
          <button
            type="button"
            onClick={() => {
              void onClearPickerFilters();
            }}
            className="ui-btn ui-btn-secondary inline-flex h-12 w-full items-center justify-center gap-2 px-4 text-center text-sm"
          >
            <GriddyIcon name="close-circle" />
            Clear
          </button>
        </form>

        {pickerError ? <p className="mt-3 text-sm text-red-700">{pickerError}</p> : null}

        {pickerLoading ? (
          <div className="flex flex-1 items-center justify-center text-sm text-slate-600">Loading products...</div>
        ) : !pickerData || pickerData.products.length === 0 ? (
          <div className="flex flex-1 items-center justify-center text-sm text-slate-600">No products found.</div>
        ) : (
          <>
            <div className="mt-3 flex items-center justify-between gap-2 rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-xs text-slate-600">
              <p>
                Showing <strong>{pickerData.products.length}</strong> / <strong>{pickerData.totalElements}</strong>
              </p>
              <p>
                Page <strong>{pickerData.currentPage + 1}</strong> / <strong>{Math.max(1, pickerData.totalPages)}</strong>
              </p>
            </div>

            <div
              className={
                pickerMotionPhase === "entering"
                  ? "compare-picker-flip-stage is-entering mt-3 flex-1 overflow-y-auto pr-1"
                  : "compare-picker-flip-stage mt-3 flex-1 overflow-y-auto pr-1"
              }
            >
              <div className="space-y-2">
              {pickerData.products.map((product, index) => {
                const inCompare = !!product.id && (compare?.ids ?? []).includes(product.id);
                const sameAsTarget = !!product.id && !!targetItem?.id && targetItem.id === product.id;
                const selectable = !!product.id && (!inCompare || sameAsTarget);

                return (
                  <article
                    key={product.id ?? `${product.name}-${product.brand}`}
                    className="compare-picker-flip-card rounded-xl border border-[var(--color-border)] bg-white p-2.5"
                    style={{ "--i": index } as CSSProperties}
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex min-w-0 flex-1 items-center gap-3">
                        <Image
                          src={toAssetUrl(product.imageUrl)}
                          alt={product.name}
                          width={56}
                          height={56}
                          sizes="56px"
                          className="h-14 w-14 rounded-lg bg-[var(--color-surface-soft)] object-contain p-1"
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold text-slate-900">{product.name}</p>
                          <p className="mt-0.5 text-xs text-slate-600">
                            {product.storage || "N/A"} / {product.ram || "N/A"}
                          </p>
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
                        className="ui-btn ui-btn-primary inline-flex shrink-0 items-center justify-center gap-2 px-3 py-2 text-xs"
                      >
                        <GriddyIcon name={sameAsTarget ? "check" : "clipboard"} />
                        {sameAsTarget
                          ? "Selected"
                          : inCompare
                            ? "Already in compare"
                            : `Add to Product ${slotIndex + 1}`}
                      </button>
                    </div>
                  </article>
                );
              })}
              </div>
            </div>

            {pickerData.totalPages > 1 ? (
              <div className="mt-3 flex flex-wrap items-center justify-center gap-2 border-t border-[var(--color-border)] pt-3">
                <button
                  type="button"
                  disabled={pickerData.currentPage === 0 || pickerLoading}
                  onClick={() => {
                    void onChangePickerPage(Math.max(0, pickerData.currentPage - 1));
                  }}
                  className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-2 text-sm"
                >
                  <GriddyIcon name="arrow-left" />
                  Previous
                </button>
                {Array.from({ length: pickerData.totalPages }).map((_, pageIndex) => (
                  <button
                    key={`picker-page-${pageIndex}`}
                    type="button"
                    disabled={pickerLoading}
                    onClick={() => {
                      void onChangePickerPage(pageIndex);
                    }}
                    className={`ui-btn inline-flex min-w-10 items-center justify-center px-3 py-2 text-sm ${
                      pageIndex === pickerData.currentPage ? "ui-btn-primary" : "ui-btn-secondary"
                    }`}
                  >
                    {pageIndex + 1}
                  </button>
                ))}
                <button
                  type="button"
                  disabled={pickerData.currentPage >= pickerData.totalPages - 1 || pickerLoading}
                  onClick={() => {
                    void onChangePickerPage(Math.min(pickerData.totalPages - 1, pickerData.currentPage + 1));
                  }}
                  className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-2 text-sm"
                >
                  Next
                  <GriddyIcon name="arrow-right" />
                </button>
              </div>
            ) : null}
          </>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Compare Products</h1>
            <p className="mt-2 text-sm text-slate-600">Limit: {maxCompare} products. Add directly at each compare position.</p>
          </div>
          {items.length > 0 ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => void mutate(() => clearCompare(), createEmptySlotIds(maxCompare))}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
            >
              <GriddyIcon name="close-circle" />
              Clear Compare List
            </button>
          ) : null}
        </div>
      </header>

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {slots.map((item, index) =>
          item ? (
            <article key={item.id ?? `${item.name}-${index}`} className="glass-panel rounded-3xl p-4">
              <div className="relative">
              <div className="mb-2 flex items-center justify-between gap-2">
                <p className="text-xs uppercase tracking-[0.12em] text-slate-500">{`Product ${index + 1}`}</p>
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
                sizes="(max-width: 768px) 90vw, (max-width: 1200px) 42vw, 30vw"
                className="aspect-square w-full rounded-2xl bg-[var(--color-surface-soft)] object-contain p-2"
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
                {item.id ? (
                  <Link
                    href={`/products/${item.id}`}
                    className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
                  >
                    <GriddyIcon name="box" />
                    View
                  </Link>
                ) : (
                  <button
                    type="button"
                    disabled
                    className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
                  >
                    <GriddyIcon name="box" />
                    View
                  </button>
                )}
                {item.id ? (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      void mutate(
                        () => removeCompareItem(item.id ?? 0),
                        normalizedSlotIds.map((slotId) => (slotId === item.id ? null : slotId)),
                      )
                    }
                    className="ui-btn ui-btn-danger inline-flex items-center gap-2 px-4 py-2 text-sm"
                  >
                    <GriddyIcon name="trash" />
                    Remove
                  </button>
                ) : null}
              </div>
              {renderPickerPanel(index)}
              </div>
            </article>
          ) : (
            <article
              key={`slot-${index}`}
              className="glass-panel relative flex min-h-[420px] flex-col items-center justify-center rounded-3xl border border-dashed border-[var(--color-border-2)] p-4 text-center"
            >
              <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full border border-[var(--color-border-2)] bg-[var(--color-surface-soft)]">
                <GriddyIcon name="package" />
              </div>
              <p className="text-sm font-semibold text-slate-900">{`Product ${index + 1} is empty`}</p>
              <p className="mt-1 max-w-[240px] text-xs text-slate-600">Pick any product from catalog and add it to this compare position.</p>
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
              {renderPickerPanel(index)}
            </article>
          ),
        )}
      </div>

      {items.length > 0 ? (
        <section className="glass-panel overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-b from-[#111111] to-[#070707] shadow-[0_14px_34px_rgba(0,0,0,0.45)]">
          <div className="border-b border-white/10 bg-[#161616] px-5 py-4">
            <h2 className="text-lg font-semibold text-slate-100">Comparison Matrix</h2>
            <p className="mt-1 text-sm text-slate-300">
              Red spec labels mean different values. Green labels mean matching values.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full border-collapse">
              <thead className="bg-[#0f0f0f]">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.08em] text-slate-300">
                    Spec
                  </th>
                  {slots.map((product, index) => (
                    <th
                      key={`compare-head-${index}`}
                      className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.08em] text-slate-300"
                    >
                      {product ? product.name : `Product ${index + 1}`}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {comparisonRows.map((row) => (
                  <tr key={row.label} className="bg-[#0b0b0b]">
                    <th
                      className={`whitespace-nowrap border-t border-white/10 px-4 py-3 text-left text-sm font-semibold ${
                        row.different
                          ? "text-red-400"
                          : "text-green-400"
                      }`}
                    >
                      <span>{row.label}</span>
                    </th>
                    {row.values.map((value, index) => (
                      <td
                        key={`${row.label}-${index}`}
                        className={`border-t border-white/10 px-4 py-3 text-sm ${row.different ? "font-semibold text-zinc-100" : "text-zinc-300"}`}
                      >
                        {value}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}


