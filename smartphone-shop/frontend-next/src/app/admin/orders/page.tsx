/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import {
  ApiError,
  fetchAdminOrders,
  shipAdminOrder,
  updateAdminOrderStatus,
  type AdminOrderPageResponse,
} from "@/lib/api";
import { formatDateTime, formatPriceVnd } from "@/lib/format";
import { PaymentMethodBadge } from "@/components/storefront/payment-method-badge";
import { getOrderStatusBadge } from "@/lib/order-status";
import { FilterDropdown, type FilterDropdownOption } from "@/components/storefront/filter-dropdown";

const ORDER_STATUS_OPTIONS: FilterDropdownOption[] = [
  { value: "pending", label: "Pending" },
  { value: "processing", label: "Processing" },
  { value: "shipped", label: "Shipped" },
  { value: "delivered", label: "Delivered" },
  { value: "completed", label: "Completed" },
  { value: "cancelled", label: "Cancelled" },
  { value: "return_requested", label: "Return requested" },
  { value: "return_approved", label: "Return approved" },
  { value: "return_rejected", label: "Return rejected" },
  { value: "refunded", label: "Refunded" },
];

function isInstallmentPlan(plan: string | null | undefined): boolean {
  return (plan ?? "").trim().toUpperCase().replace(/\s+/g, "_") === "INSTALLMENT";
}

function paymentPlanLabel(plan: string | null | undefined): string {
  return isInstallmentPlan(plan) ? "Installment" : "Full payment";
}

export default function AdminOrdersPage() {
  const [data, setData] = useState<AdminOrderPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null);
  const [busyShipOrderId, setBusyShipOrderId] = useState<number | null>(null);
  const [trackingInputs, setTrackingInputs] = useState<Record<number, { trackingNumber: string; carrier: string }>>({});
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

  async function onShip(orderId: number) {
    const input = trackingInputs[orderId];
    if (!input?.trackingNumber?.trim() || !input?.carrier?.trim()) {
      setError("Tracking number and carrier are required.");
      return;
    }

    setBusyShipOrderId(orderId);
    setError(null);
    setMessage(null);
    try {
      const result = await shipAdminOrder(orderId, {
        trackingNumber: input.trackingNumber.trim(),
        carrier: input.carrier.trim(),
      });
      setMessage(result.message);
      await loadOrders();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to ship order.");
      }
    } finally {
      setBusyShipOrderId(null);
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
          {data.orders.map((order) => {
            const installment = isInstallmentPlan(order.paymentPlan);
            const statusBadge = getOrderStatusBadge(order.status);
            return (
              <article key={order.id} className="glass-panel relative rounded-3xl p-5 focus-within:z-40">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-lg font-semibold text-slate-900">{order.orderCode}</p>
                    <p className="text-sm text-slate-600">
                      {order.customerName} - {formatDateTime(order.createdAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <p className="text-lg font-bold text-slate-900">{formatPriceVnd(order.totalAmount)}</p>
                    <span
                      className={[
                        "rounded-full border px-2.5 py-1 text-xs font-semibold",
                        statusBadge.className,
                      ].join(" ")}
                    >
                      {statusBadge.label}
                    </span>
                  </div>
                </div>

                <p className="mt-2 text-sm text-slate-700">{order.statusSummary}</p>
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
                {order.trackingNumber ? (
                  <p className="mt-1 text-xs text-slate-600">
                    Tracking: {order.trackingCarrier ? `${order.trackingCarrier} - ` : ""}
                    {order.trackingNumber}
                  </p>
                ) : null}

                {installment && typeof order.installmentMonths === "number" && typeof order.installmentMonthlyAmount === "number" ? (
                  <p className="mt-1 text-xs text-amber-700">
                    Installment: {order.installmentMonths} months x {formatPriceVnd(order.installmentMonthlyAmount)} / month
                  </p>
                ) : null}

                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <span className="text-sm font-medium text-slate-300">Status</span>
                  <div className="w-[220px] max-w-full">
                    <FilterDropdown
                      options={ORDER_STATUS_OPTIONS}
                      value={order.status}
                      onChange={(nextValue) => void onStatusChange(order.id, nextValue)}
                      disabled={busyOrderId === order.id}
                      triggerClassName={statusBadge.className}
                    />
                  </div>
                </div>

                {order.status === "processing" ? (
                  <div className="mt-3 flex flex-wrap items-end gap-2">
                    <label className="flex flex-col gap-1 text-xs text-slate-600">
                      Tracking #
                      <input
                        value={trackingInputs[order.id]?.trackingNumber ?? ""}
                        onChange={(event) =>
                          setTrackingInputs((prev) => ({
                            ...prev,
                            [order.id]: {
                              trackingNumber: event.target.value,
                              carrier: prev[order.id]?.carrier ?? "",
                            },
                          }))
                        }
                        className="rounded-lg border border-slate-300 px-2 py-1 text-sm text-slate-900"
                        placeholder="JP123456789"
                      />
                    </label>
                    <label className="flex flex-col gap-1 text-xs text-slate-600">
                      Carrier
                      <input
                        value={trackingInputs[order.id]?.carrier ?? ""}
                        onChange={(event) =>
                          setTrackingInputs((prev) => ({
                            ...prev,
                            [order.id]: {
                              trackingNumber: prev[order.id]?.trackingNumber ?? "",
                              carrier: event.target.value,
                            },
                          }))
                        }
                        className="rounded-lg border border-slate-300 px-2 py-1 text-sm text-slate-900"
                        placeholder="Yamato"
                      />
                    </label>
                    <button
                      type="button"
                      className="rounded-lg bg-slate-900 px-3 py-2 text-xs font-semibold text-white disabled:opacity-60"
                      onClick={() => void onShip(order.id)}
                      disabled={busyShipOrderId === order.id}
                    >
                      {busyShipOrderId === order.id ? "Shipping..." : "Ship with tracking"}
                    </button>
                  </div>
                ) : null}
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

