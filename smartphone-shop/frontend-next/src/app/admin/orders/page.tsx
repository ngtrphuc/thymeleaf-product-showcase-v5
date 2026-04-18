/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import {
  ApiError,
  fetchAdminOrders,
  updateAdminOrderStatus,
  type AdminOrderPageResponse,
} from "@/lib/api";
import { formatDateTime, formatPriceVnd } from "@/lib/format";

const ORDER_STATUSES = ["pending", "processing", "shipped", "delivered", "cancelled"];

export default function AdminOrdersPage() {
  const [data, setData] = useState<AdminOrderPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function loadOrders() {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAdminOrders(0, 20);
      setData(response);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load admin orders.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadOrders();
  }, []);

  async function onStatusChange(orderId: number, status: string) {
    setBusyOrderId(orderId);
    setError(null);
    setMessage(null);

    try {
      const result = await updateAdminOrderStatus(orderId, status);
      setMessage(result.message);
      await loadOrders();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to update status.");
      }
    } finally {
      setBusyOrderId(null);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading admin orders...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Admin Orders</h1>
        <p className="mt-2 text-sm text-slate-600">Review and update lifecycle status of customer orders.</p>
      </header>

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {!data || data.orders.length === 0 ? (
        <div className="glass-panel rounded-3xl p-8 text-center text-slate-700">No orders found.</div>
      ) : (
        <div className="space-y-4">
          {data.orders.map((order) => (
            <article key={order.id} className="glass-panel rounded-3xl p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-lg font-semibold text-slate-900">{order.orderCode}</p>
                  <p className="text-sm text-slate-600">
                    {order.customerName} - {formatDateTime(order.createdAt)}
                  </p>
                </div>
                <p className="text-lg font-bold text-slate-900">{formatPriceVnd(order.totalAmount)}</p>
              </div>

              <p className="mt-2 text-sm text-slate-700">{order.statusSummary}</p>
              <p className="mt-1 text-sm text-slate-700">Shipping: {order.shippingAddress}</p>

              <div className="mt-3 flex flex-wrap items-center gap-2">
                <span className="text-sm text-slate-700">Status:</span>
                <select
                  value={order.status}
                  onChange={(event) => void onStatusChange(order.id, event.target.value)}
                  disabled={busyOrderId === order.id}
                  className="ui-input px-3 py-1.5 text-sm"
                >
                  {ORDER_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

