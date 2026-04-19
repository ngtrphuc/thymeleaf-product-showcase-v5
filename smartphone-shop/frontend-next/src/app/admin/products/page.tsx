/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Image from "next/image";
import { FormEvent, useEffect, useState } from "react";
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

export default function AdminProductsPage() {
  const [data, setData] = useState<AdminProductPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [newProduct, setNewProduct] = useState<AdminProduct>(EMPTY_PRODUCT);
  const [editId, setEditId] = useState<number | null>(null);
  const [editProduct, setEditProduct] = useState<AdminProduct>(EMPTY_PRODUCT);

  async function loadProducts() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("page", "0");
      params.set("pageSize", "20");
      const response = await fetchAdminProducts(params);
      setData(response);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load admin products.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadProducts();
  }, []);

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
        <h2 className="text-lg font-semibold text-slate-900">Product List</h2>
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
