"use client";

import { useEffect, useState } from "react";
import { ApiError, cancelOrder, fetchOrders, type OrderPageResponse, type OrderResponse } from "@/lib/api";
import { formatDateTime, formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";
import { PaymentMethodBadge } from "@/components/storefront/payment-method-badge";
import { getOrderStatusBadge } from "@/lib/order-status";

function isInstallmentPlan(plan: string | null | undefined): boolean {
  return (plan ?? "").trim().toUpperCase().replace(/\s+/g, "_") === "INSTALLMENT";
}

function paymentPlanLabel(plan: string | null | undefined): string {
  return isInstallmentPlan(plan) ? "Installment" : "Full payment";
}

export default function OrdersPage() {
  const [orderPage, setOrderPage] = useState<OrderPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  async function loadOrders(page = currentPage) {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchOrders(page, pageSize);
      setOrderPage(data);
      setCurrentPage(data.currentPage);
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
    const timer = window.setTimeout(() => {
      void loadOrders(0);
    }, 0);
    return () => {
      window.clearTimeout(timer);
    };
    // We intentionally load once on mount, page changes are explicit via controls.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onCancel(orderId: number) {
    setBusyOrderId(orderId);
    setError(null);
    setMessage(null);
    try {
      const result = await cancelOrder(orderId);
      setMessage(result.message);
      await loadOrders(currentPage);
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

  const orders: OrderResponse[] = orderPage?.orders ?? [];
  const totalPages = orderPage?.totalPages ?? 0;

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
          {orders.map((order) => {
            const installment = isInstallmentPlan(order.paymentPlan);
            const statusBadge = getOrderStatusBadge(order.status);
            const hasInstallmentBreakdown =
              installment &&
              typeof order.installmentMonths === "number" &&
              order.installmentMonths > 0 &&
              typeof order.installmentMonthlyAmount === "number" &&
              order.installmentMonthlyAmount > 0;

            return (
              <article key={order.id} className="glass-panel rounded-3xl p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-lg font-semibold text-slate-900">{order.orderCode}</p>
                    <p className="text-sm text-slate-600">{formatDateTime(order.createdAt)}</p>
                  </div>
                  <span
                    className={[
                      "rounded-full border px-3 py-1 text-xs font-semibold",
                      statusBadge.className,
                    ].join(" ")}
                  >
                    {statusBadge.label}
                  </span>
                </div>

                <p className="mt-3 text-sm text-slate-700">{order.statusSummary}</p>
                <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-700">
                  <span>Payment:</span>
                  <PaymentMethodBadge
                    method={order.paymentMethod}
                    label={order.paymentMethod}
                    textClassName="font-semibold"
                  />
                  <span
                    className={[
                      "rounded-full px-2 py-0.5 text-xs font-semibold",
                      installment
                        ? "bg-amber-100 text-amber-700"
                        : "bg-emerald-100 text-emerald-700",
                    ].join(" ")}
                  >
                    {paymentPlanLabel(order.paymentPlan)}
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-700">Shipping: {order.shippingAddress}</p>
                <p className="mt-2 text-xl font-bold text-slate-900">{formatPriceVnd(order.totalAmount)}</p>

                {installment ? (
                  <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">Installment Plan</p>
                    {hasInstallmentBreakdown ? (
                      <p className="mt-1 text-sm text-amber-800">
                        {order.installmentMonths} months x {formatPriceVnd(order.installmentMonthlyAmount ?? 0)} / month
                      </p>
                    ) : (
                      <p className="mt-1 text-sm text-amber-800">Installment details are being prepared.</p>
                    )}
                  </div>
                ) : (
                  <div className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 p-3 shadow-[0_8px_20px_rgba(16,185,129,0.22)]">
                    <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">One-Time Payment</p>
                    <p className="mt-1 text-sm text-emerald-800">Paid in full at checkout.</p>
                  </div>
                )}

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
            );
          })}
        </div>
      )}

      {totalPages > 1 ? (
        <div className="flex items-center justify-end gap-2">
          <button
            type="button"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-sm"
            disabled={currentPage <= 0 || loading}
            onClick={() => void loadOrders(Math.max(0, currentPage - 1))}
          >
            Previous
          </button>
          <span className="text-sm text-[var(--color-text-muted)]">
            Page {currentPage + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-sm"
            disabled={currentPage >= totalPages - 1 || loading}
            onClick={() => void loadOrders(currentPage + 1)}
          >
            Next
          </button>
        </div>
      ) : null}
    </div>
  );
}
