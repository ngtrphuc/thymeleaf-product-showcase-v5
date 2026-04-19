/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  createAdminProduct,
  deleteAdminProduct,
  fetchAdminProducts,
  toAssetUrl,
  updateAdminProduct,
  type AdminProduct,
  type AdminProductPageResponse,
} from "@/lib/api";
import { FilterDropdown, type FilterDropdownOption } from "@/components/storefront/filter-dropdown";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

const EMPTY_PRODUCT: AdminProduct = {
  id: null,
  name: "",
  price: 0,
  imageUrl: "",
  stock: 0,
  os: "",
  chipset: "",
  speed: "",
  ram: "",
  storage: "",
  size: "",
  resolution: "",
  battery: "",
  charging: "",
  description: "",
};

const ADMIN_PRODUCT_PAGE_SIZE = 10;
const ADMIN_SORT_OPTIONS: FilterDropdownOption[] = [
  { label: "Newest", value: "id_desc" },
  { label: "Name A-Z", value: "name_asc" },
  { label: "Name Z-A", value: "name_desc" },
  { label: "Price Low to High", value: "price_asc" },
  { label: "Price High to Low", value: "price_desc" },
  { label: "Stock Low to High", value: "stock_asc" },
  { label: "Stock High to Low", value: "stock_desc" },
];

const ADMIN_STOCK_OPTIONS: FilterDropdownOption[] = [
  { label: "All stock", value: "all" },
  { label: "In stock", value: "in_stock" },
  { label: "Low stock", value: "low_stock" },
  { label: "Out of stock", value: "out_of_stock" },
];

export default function AdminProductsPage() {
  const [data, setData] = useState<AdminProductPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [newProduct, setNewProduct] = useState<AdminProduct>(EMPTY_PRODUCT);
  const [editId, setEditId] = useState<number | null>(null);
  const [editProduct, setEditProduct] = useState<AdminProduct>(EMPTY_PRODUCT);
  const [keywordInput, setKeywordInput] = useState("");
  const [brandInput, setBrandInput] = useState("");
  const [stockInput, setStockInput] = useState("all");
  const [sortInput, setSortInput] = useState("id_desc");
  const [filters, setFilters] = useState({
    keyword: "",
    brand: "",
    stock: "all",
    sort: "id_desc",
  });
  const [page, setPage] = useState(0);

  const brandOptions = useMemo<FilterDropdownOption[]>(
    () => [
      { label: "All brands", value: "" },
      ...((data?.brands ?? []).map((item) => ({ label: item, value: item })) as FilterDropdownOption[]),
    ],
    [data?.brands],
  );

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("pageSize", String(ADMIN_PRODUCT_PAGE_SIZE));
      params.set("sort", filters.sort);
      params.set("stock", filters.stock);
      if (filters.keyword.trim()) {
        params.set("keyword", filters.keyword.trim());
      }
      if (filters.brand.trim()) {
        params.set("brand", filters.brand.trim());
      }
      const response = await fetchAdminProducts(params);
      setData(response);
      if (response.currentPage !== page) {
        setPage(response.currentPage);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load admin products.");
      }
    } finally {
      setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    void loadProducts();
  }, [loadProducts]);

  function onApplyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setFilters({
      keyword: keywordInput,
      brand: brandInput,
      stock: stockInput,
      sort: sortInput,
    });
  }

  function onClearFilters() {
    setKeywordInput("");
    setBrandInput("");
    setStockInput("all");
    setSortInput("id_desc");
    setPage(0);
    setFilters({
      keyword: "",
      brand: "",
      stock: "all",
      sort: "id_desc",
    });
  }

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await createAdminProduct(newProduct);
      setMessage("Product created.");
      setNewProduct(EMPTY_PRODUCT);
      await loadProducts();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to create product.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function onUpdate() {
    if (editId === null) {
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await updateAdminProduct(editId, editProduct);
      setMessage("Product updated.");
      setEditId(null);
      await loadProducts();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to update product.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(id: number) {
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await deleteAdminProduct(id);
      setMessage("Product deleted.");
      await loadProducts();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to delete product.");
      }
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading products...</div>;
  }

  const currentPage = data?.currentPage ?? 0;
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Admin Products</h1>
        <p className="mt-2 text-sm text-slate-600">Manage inventory directly from Next.js admin UI.</p>
      </header>

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <form onSubmit={onCreate} className="glass-panel grid gap-3 rounded-3xl p-6 sm:grid-cols-2">
        <h2 className="sm:col-span-2 text-lg font-semibold text-slate-900">Create Product</h2>
        <input
          value={newProduct.name}
          onChange={(event) => setNewProduct((prev) => ({ ...prev, name: event.target.value }))}
          placeholder="Product name"
          className="ui-input px-3 py-2 text-sm"
          required
        />
        <input
          type="number"
          value={newProduct.price ?? 0}
          onChange={(event) =>
            setNewProduct((prev) => ({ ...prev, price: Number.parseFloat(event.target.value || "0") }))
          }
          placeholder="Price"
          className="ui-input px-3 py-2 text-sm"
          min={0}
          required
        />
        <input
          type="number"
          value={newProduct.stock ?? 0}
          onChange={(event) =>
            setNewProduct((prev) => ({ ...prev, stock: Number.parseInt(event.target.value || "0", 10) }))
          }
          placeholder="Stock"
          className="ui-input px-3 py-2 text-sm"
          min={0}
          required
        />
        <input
          value={newProduct.imageUrl ?? ""}
          onChange={(event) => setNewProduct((prev) => ({ ...prev, imageUrl: event.target.value }))}
          placeholder="Image URL"
          className="ui-input px-3 py-2 text-sm"
        />
        <textarea
          value={newProduct.description ?? ""}
          onChange={(event) => setNewProduct((prev) => ({ ...prev, description: event.target.value }))}
          placeholder="Description"
          className="ui-input sm:col-span-2 px-3 py-2 text-sm"
          rows={3}
        />
        <button
          type="submit"
          disabled={saving}
          className="ui-btn ui-btn-primary sm:col-span-2 inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="box" />
          {saving ? "Saving..." : "Create Product"}
        </button>
      </form>

      <section className="glass-panel rounded-3xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Product List</h2>
            <p className="mt-1 text-sm text-slate-600">
              Showing <strong>{data?.products.length ?? 0}</strong> / <strong>{totalElements}</strong> products
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              disabled={currentPage === 0}
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-2 text-sm"
            >
              <GriddyIcon name="arrow-left" />
              Previous
            </button>
            <div className="hidden gap-2 sm:flex">
              {Array.from({ length: totalPages }).map((_, index) => (
                <button
                  key={`admin-product-page-${index}`}
                  type="button"
                  onClick={() => setPage(index)}
                  className={`ui-btn inline-flex min-w-10 items-center justify-center px-3 py-2 text-sm ${
                    index === currentPage ? "ui-btn-primary" : "ui-btn-secondary"
                  }`}
                >
                  {index + 1}
                </button>
              ))}
            </div>
            <span className="text-sm text-slate-600 sm:hidden">
              Page <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong>
            </span>
            <button
              type="button"
              disabled={currentPage >= totalPages - 1}
              onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-2 text-sm"
            >
              Next
              <GriddyIcon name="arrow-right" />
            </button>
          </div>
        </div>

        <form onSubmit={onApplyFilters} className="mt-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-4">
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <input
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              placeholder="Search by name or ID"
              className="ui-input h-11 px-3 text-sm"
            />
            <FilterDropdown
              options={brandOptions}
              value={brandInput}
              onChange={setBrandInput}
              triggerClassName="h-11"
            />
            <FilterDropdown
              options={ADMIN_STOCK_OPTIONS}
              value={stockInput}
              onChange={setStockInput}
              triggerClassName="h-11"
            />
            <FilterDropdown
              options={ADMIN_SORT_OPTIONS}
              value={sortInput}
              onChange={setSortInput}
              triggerClassName="h-11"
            />
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="submit"
              className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
            >
              <GriddyIcon name="check" />
              Apply
            </button>
            <button
              type="button"
              onClick={onClearFilters}
              className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-4 py-2 text-sm"
            >
              <GriddyIcon name="close-circle" />
              Clear
            </button>
          </div>
        </form>

        {!data || data.products.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No products found.</p>
        ) : (
          <div className="mt-4 space-y-3">
            {data.products.map((product) => {
              const isEditing = editId === product.id;
              return (
                <article key={product.id ?? product.name} className="rounded-xl border border-[var(--color-border)] bg-white p-3">
                  {isEditing ? (
                    <div className="grid gap-2 sm:grid-cols-3">
                      <input
                        value={editProduct.name}
                        onChange={(event) => setEditProduct((prev) => ({ ...prev, name: event.target.value }))}
                        className="ui-input px-2 py-1.5 text-sm"
                      />
                      <input
                        type="number"
                        value={editProduct.price ?? 0}
                        onChange={(event) =>
                          setEditProduct((prev) => ({
                            ...prev,
                            price: Number.parseFloat(event.target.value || "0"),
                          }))
                        }
                        className="ui-input px-2 py-1.5 text-sm"
                      />
                      <input
                        type="number"
                        value={editProduct.stock ?? 0}
                        onChange={(event) =>
                          setEditProduct((prev) => ({
                            ...prev,
                            stock: Number.parseInt(event.target.value || "0", 10),
                          }))
                        }
                        className="ui-input px-2 py-1.5 text-sm"
                      />
                      <div className="sm:col-span-3 flex gap-2">
                        <button
                          type="button"
                          onClick={() => void onUpdate()}
                          disabled={saving}
                          className="ui-btn ui-btn-primary inline-flex items-center gap-1.5 px-3 py-1.5 text-xs"
                        >
                          <GriddyIcon name="check" />
                          Save
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditId(null)}
                          className="ui-btn ui-btn-secondary inline-flex items-center gap-1.5 px-3 py-1.5 text-xs"
                        >
                          <GriddyIcon name="ban" />
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="flex min-w-0 items-center gap-3">
                        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-soft)]">
                          <Image
                            src={toAssetUrl(product.imageUrl)}
                            alt={product.name}
                            fill
                            className="object-contain p-1.5"
                            sizes="64px"
                            unoptimized
                          />
                        </div>
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-slate-900">{product.name}</p>
                          <p className="text-sm text-slate-600">
                            {formatPriceVnd(product.price)} - Stock: {product.stock}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditId(product.id ?? null);
                            setEditProduct(product);
                          }}
                          className="ui-btn ui-btn-secondary inline-flex items-center gap-1.5 px-3 py-1.5 text-xs"
                        >
                          <GriddyIcon name="clipboard" />
                          Edit
                        </button>
                        {product.id ? (
                          <button
                            type="button"
                            onClick={() => void onDelete(product.id ?? 0)}
                            className="ui-btn ui-btn-danger inline-flex items-center gap-1.5 px-3 py-1.5 text-xs"
                          >
                            <GriddyIcon name="trash" />
                            Delete
                          </button>
                        ) : null}
                      </div>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
