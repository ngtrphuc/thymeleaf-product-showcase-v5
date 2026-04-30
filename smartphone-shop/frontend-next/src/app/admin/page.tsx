"use client";

import { useEffect, useState } from "react";
import { ApiError, fetchAdminDashboard, type AdminDashboardResponse } from "@/lib/api";
import { formatDateTime, formatPriceVnd } from "@/lib/format";
import { getOrderStatusBadge } from "@/lib/order-status";

export default function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminDashboard(0);
        setData(response);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError("Failed to load admin dashboard.");
        }
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <header className="glass-panel rounded-3xl p-6">
          <div className="ui-skeleton h-8 w-44 rounded-lg" />
          <div className="ui-skeleton mt-3 h-4 w-72 rounded-lg" />
        </header>
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="glass-panel rounded-2xl p-4">
              <div className="ui-skeleton h-3 w-20 rounded" />
              <div className="ui-skeleton mt-3 h-8 w-24 rounded-lg" />
            </div>
          ))}
        </section>
        <section className="glass-panel rounded-3xl p-6">
          <div className="ui-skeleton h-6 w-32 rounded-lg" />
          <div className="mt-4 space-y-3">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="ui-skeleton h-12 rounded-xl" />
            ))}
          </div>
        </section>
      </div>
    );
  }

  if (!data) {
    return <div className="glass-panel rounded-3xl p-8 text-center text-red-700">{error ?? "No data."}</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Admin Dashboard</h1>
        <p className="mt-2 text-sm text-slate-600">Operational summary from backend admin APIs.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="glass-panel rounded-2xl p-4">
          <p className="text-xs uppercase text-slate-500">Products</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{data.totalProducts}</p>
        </div>
        <div className="glass-panel rounded-2xl p-4">
          <p className="text-xs uppercase text-slate-500">Orders</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{data.totalOrders}</p>
        </div>
        <div className="glass-panel rounded-2xl p-4">
          <p className="text-xs uppercase text-slate-500">Items Sold</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{data.totalItemsSold}</p>
        </div>
        <div className="glass-panel rounded-2xl p-4">
          <p className="text-xs uppercase text-slate-500">Revenue</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{formatPriceVnd(data.totalRevenue)}</p>
        </div>
      </section>

      <section className="glass-panel rounded-3xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">Recent Orders</h2>
        {data.recentOrders.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No orders yet.</p>
        ) : (
          <div className="mt-4 space-y-3">
            {data.recentOrders.map((order) => {
              const statusBadge = getOrderStatusBadge(order.status);
              return (
                <article key={order.id} className="rounded-xl border border-[var(--color-border)] bg-white p-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-semibold text-slate-900">{order.orderCode}</p>
                    <span
                      className={[
                        "rounded-full border px-2.5 py-1 text-xs font-semibold",
                        statusBadge.className,
                      ].join(" ")}
                    >
                      {statusBadge.label}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">
                    {order.customerName} | {formatDateTime(order.createdAt)}
                  </p>
                  <p className="mt-1 text-sm font-semibold text-slate-900">{formatPriceVnd(order.totalAmount)}</p>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
