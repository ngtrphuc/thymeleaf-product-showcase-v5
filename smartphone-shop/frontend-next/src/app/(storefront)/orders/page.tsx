/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import { ApiError, cancelOrder, fetchOrders, type OrderResponse } from "@/lib/api";
import { formatDateTime, formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";
import { PaymentMethodBadge } from "@/components/storefront/payment-method-badge";

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null);

  async function loadOrders() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchOrders();
      setOrders(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load orders.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadOrders();
  }, []);

  async function onCancel(orderId: number) {
    setBusyOrderId(orderId);
    setError(null);
    setMessage(null);
    try {
      const result = await cancelOrder(orderId);
      setMessage(result.message);
      await loadOrders();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to cancel order.");
      }
    } finally {
      setBusyOrderId(null);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading orders...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">My Orders</h1>
        <p className="mt-2 text-sm text-slate-600">Track and manage your order history.</p>
      </header>

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {orders.length === 0 ? (
        <div className="glass-panel rounded-3xl p-8 text-center text-slate-700">No orders yet.</div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <article key={order.id} className="glass-panel rounded-3xl p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-lg font-semibold text-slate-900">{order.orderCode}</p>
                  <p className="text-sm text-slate-600">{formatDateTime(order.createdAt)}</p>
                </div>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                  {order.status}
                </span>
              </div>

              <p className="mt-3 text-sm text-slate-700">{order.statusSummary}</p>
              <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-700">
                <span>Payment:</span>
                <PaymentMethodBadge
                  method={order.paymentMethod}
                  label={order.paymentMethod}
                  textClassName="font-semibold text-slate-900"
                />
                <span>- {order.paymentPlan}</span>
              </div>
              <p className="mt-1 text-sm text-slate-700">Shipping: {order.shippingAddress}</p>
              <p className="mt-2 text-xl font-bold text-slate-900">{formatPriceVnd(order.totalAmount)}</p>

              <div className="mt-3 space-y-1 text-sm text-slate-700">
                {order.items.map((item) => (
                  <p key={`${order.id}-${item.productId}`}>
                    {item.productName} x {item.quantity}
                  </p>
                ))}
              </div>

              {order.cancelable ? (
                <button
                  type="button"
                  disabled={busyOrderId === order.id}
                  onClick={() => void onCancel(order.id)}
                  className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 disabled:opacity-60"
                >
                  <GriddyIcon name="ban" />
                  {busyOrderId === order.id ? "Cancelling..." : "Cancel Order"}
                </button>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

