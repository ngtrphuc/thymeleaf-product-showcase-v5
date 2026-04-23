/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ApiError,
  fetchCart,
  fetchPaymentMethods,
  fetchProfile,
  placeOrder,
  type CartResponse,
  type PaymentMethodResponse,
} from "@/lib/api";
import { CheckoutSkeleton } from "@/components/storefront/checkout-skeleton";
import { formatPriceVnd } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";
import { PaymentMethodBadge } from "@/components/storefront/payment-method-badge";
import { FilterDropdown, type FilterDropdownOption } from "@/components/storefront/filter-dropdown";

type PaymentMethodType = "CASH_ON_DELIVERY" | "BANK_TRANSFER" | "PAYPAY" | "MASTERCARD";
type PaymentPlanType = "FULL_PAYMENT" | "INSTALLMENT";
type ShippingAddressMode = "SAVED" | "NEW";
type PaymentSelectionMode = "SAVED" | "OTHER";

const PAYMENT_METHOD_OPTIONS: FilterDropdownOption[] = [
  { label: "Cash on Delivery", value: "CASH_ON_DELIVERY" },
  { label: "Bank Transfer", value: "BANK_TRANSFER" },
  { label: "PayPay", value: "PAYPAY" },
  { label: "Credit Card", value: "MASTERCARD" },
];

const PAYMENT_PLAN_OPTIONS: FilterDropdownOption[] = [
  { label: "Full Payment", value: "FULL_PAYMENT" },
  { label: "Installment", value: "INSTALLMENT" },
];

const INSTALLMENT_MONTH_OPTIONS: FilterDropdownOption[] = [
  { label: "6 months", value: "6" },
  { label: "12 months", value: "12" },
  { label: "24 months", value: "24" },
];

function createIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `checkout-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [customerName, setCustomerName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [savedShippingAddress, setSavedShippingAddress] = useState("");
  const [newShippingAddress, setNewShippingAddress] = useState("");
  const [shippingAddressMode, setShippingAddressMode] = useState<ShippingAddressMode>("NEW");
  const [paymentSelectionMode, setPaymentSelectionMode] = useState<PaymentSelectionMode>("OTHER");
  const [selectedSavedPaymentId, setSelectedSavedPaymentId] = useState<number | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodType>("CASH_ON_DELIVERY");
  const [paymentDetail, setPaymentDetail] = useState("");
  const [paymentPlan, setPaymentPlan] = useState<PaymentPlanType>("FULL_PAYMENT");
  const [installmentMonths, setInstallmentMonths] = useState<number>(24);
  const [idempotencyKey, setIdempotencyKey] = useState(() => createIdempotencyKey());

  function asPaymentMethodType(input: string): PaymentMethodType | null {
    if (input === "CASH_ON_DELIVERY" || input === "BANK_TRANSFER" || input === "PAYPAY" || input === "MASTERCARD") {
      return input;
    }
    return null;
  }

  const loadCheckoutData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [cartData, profileData, paymentData] = await Promise.all([
        fetchCart(),
        fetchProfile(),
        fetchPaymentMethods(),
      ]);
      setCart(cartData);
      setPaymentMethods(paymentData);
      setCustomerName(profileData.fullName ?? "");
      setPhoneNumber(profileData.phoneNumber ?? "");
      const defaultAddress = profileData.defaultAddress?.trim() ?? "";
      setSavedShippingAddress(defaultAddress);
      setShippingAddressMode(defaultAddress.length > 0 ? "SAVED" : "NEW");

      const defaultMethod = paymentData.find((method) => method.isDefault);
      if (defaultMethod) {
        const methodType = asPaymentMethodType(defaultMethod.type);
        if (methodType) {
          setPaymentMethod(methodType);
          setPaymentSelectionMode("SAVED");
          setSelectedSavedPaymentId(defaultMethod.id);
        }
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load checkout data.");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCheckoutData();
  }, [loadCheckoutData]);

  const isInstallmentDisabled = paymentMethod === "CASH_ON_DELIVERY";

  useEffect(() => {
    if (isInstallmentDisabled) {
      setPaymentPlan("FULL_PAYMENT");
    }
  }, [isInstallmentDisabled]);

  const hasSavedShippingAddress = savedShippingAddress.trim().length > 0;
  const resolvedShippingAddress = (shippingAddressMode === "SAVED" ? savedShippingAddress : newShippingAddress).trim();
  const savedPaymentMethods = useMemo(
    () => paymentMethods.filter((method) => asPaymentMethodType(method.type) !== null),
    [paymentMethods],
  );
  const selectedSavedPayment = useMemo(
    () => savedPaymentMethods.find((method) => method.id === selectedSavedPaymentId) ?? null,
    [savedPaymentMethods, selectedSavedPaymentId],
  );
  const otherPaymentMethodOptions = useMemo(() => {
    const savedTypes = new Set<PaymentMethodType>();
    for (const method of savedPaymentMethods) {
      const methodType = asPaymentMethodType(method.type);
      if (methodType) {
        savedTypes.add(methodType);
      }
    }
    const filtered = PAYMENT_METHOD_OPTIONS.filter((option) => !savedTypes.has(option.value as PaymentMethodType));
    return filtered.length > 0 ? filtered : PAYMENT_METHOD_OPTIONS;
  }, [savedPaymentMethods]);
  const currentPaymentLabel =
    paymentSelectionMode === "SAVED" && selectedSavedPayment?.displayName
      ? selectedSavedPayment.displayName
      : undefined;
  const currentPaymentTypeLabel = PAYMENT_METHOD_OPTIONS.find((option) => option.value === paymentMethod)?.label ?? "Payment";

  const installmentMonthly = useMemo(() => {
    if (!cart || paymentPlan !== "INSTALLMENT") {
      return null;
    }
    return Math.round((cart.totalAmount || 0) / installmentMonths);
  }, [cart, paymentPlan, installmentMonths]);
  const isInstallmentSelected = paymentPlan === "INSTALLMENT";
  const paymentPlanLabel = isInstallmentSelected ? "Installment Plan" : "One-Time Payment";

  const isSubmitDisabled =
    submitting ||
    customerName.trim().length === 0 ||
    phoneNumber.trim().length === 0 ||
    resolvedShippingAddress.length === 0 ||
    (paymentMethod === "BANK_TRANSFER" && paymentDetail.trim().length === 0);

  function onSelectSavedPayment(method: PaymentMethodResponse) {
    const methodType = asPaymentMethodType(method.type);
    if (!methodType) {
      return;
    }
    setPaymentSelectionMode("SAVED");
    setSelectedSavedPaymentId(method.id);
    setPaymentMethod(methodType);
  }

  function switchToOtherPayment() {
    setPaymentSelectionMode("OTHER");
    setSelectedSavedPaymentId(null);
    const exists = otherPaymentMethodOptions.some((option) => option.value === paymentMethod);
    if (!exists && otherPaymentMethodOptions.length > 0) {
      setPaymentMethod(otherPaymentMethodOptions[0].value as PaymentMethodType);
    }
  }

  function switchToSavedPayment() {
    if (savedPaymentMethods.length === 0) {
      return;
    }
    const currentSelected = savedPaymentMethods.find((method) => method.id === selectedSavedPaymentId) ?? savedPaymentMethods[0];
    setPaymentSelectionMode("SAVED");
    setSelectedSavedPaymentId(currentSelected.id);
    const methodType = asPaymentMethodType(currentSelected.type);
    if (methodType) {
      setPaymentMethod(methodType);
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    if (resolvedShippingAddress.length === 0) {
      setError("Please provide a shipping address.");
      setSubmitting(false);
      return;
    }

    try {
      const created = await placeOrder({
        customerName: customerName.trim(),
        phoneNumber: phoneNumber.trim(),
        shippingAddress: resolvedShippingAddress,
        paymentMethod,
        paymentDetail: paymentMethod === "BANK_TRANSFER" ? paymentDetail.trim() : null,
        paymentPlan,
        installmentMonths: paymentPlan === "INSTALLMENT" ? installmentMonths : null,
      }, idempotencyKey);
      setIdempotencyKey(createIdempotencyKey());
      const successParams = new URLSearchParams({
        code: created.orderCode,
      });
      router.replace(`/checkout/success?${successParams.toString()}`);
      return;
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Checkout failed.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <CheckoutSkeleton />;
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="glass-panel rounded-3xl p-8 text-center">
        <p className="text-slate-700">Your cart is empty. Add items before checkout.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Checkout</h1>
        <p className="mt-2 text-sm text-slate-600">Complete shipping and payment to place your order.</p>
      </header>

      {error ? (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {error}
        </div>
      ) : null}

      <form className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px] xl:grid-cols-[minmax(0,1fr)_460px]" onSubmit={onSubmit}>
        <section className="glass-panel space-y-6 rounded-3xl p-6">
          <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-4">
            <h2 className="text-lg font-semibold text-slate-900">Contact</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block space-y-1">
                <span className="text-sm text-slate-700">Full Name</span>
                <input
                  value={customerName}
                  onChange={(event) => setCustomerName(event.target.value)}
                  className="ui-input w-full px-3 py-2 text-sm"
                  required
                />
              </label>
              <label className="block space-y-1">
                <span className="text-sm text-slate-700">Phone Number</span>
                <input
                  value={phoneNumber}
                  onChange={(event) => setPhoneNumber(event.target.value)}
                  className="ui-input w-full px-3 py-2 text-sm"
                  required
                />
              </label>
            </div>
          </div>

          <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-4">
            <h2 className="text-lg font-semibold text-slate-900">Shipping</h2>

            {hasSavedShippingAddress ? (
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  className={[
                    "rounded-lg border px-3 py-1.5 text-sm",
                    shippingAddressMode === "SAVED"
                      ? "border-white/40 bg-white/15 text-white"
                      : "border-[var(--color-border)] bg-transparent text-[var(--color-text-muted)] hover:border-[var(--color-border-2)]",
                  ].join(" ")}
                  onClick={() => setShippingAddressMode("SAVED")}
                >
                  Saved address
                </button>
                <button
                  type="button"
                  className={[
                    "rounded-lg border px-3 py-1.5 text-sm",
                    shippingAddressMode === "NEW"
                      ? "border-white/40 bg-white/15 text-white"
                      : "border-[var(--color-border)] bg-transparent text-[var(--color-text-muted)] hover:border-[var(--color-border-2)]",
                  ].join(" ")}
                  onClick={() => setShippingAddressMode("NEW")}
                >
                  Ship to new address
                </button>
              </div>
            ) : (
              <p className="text-sm text-[var(--color-text-muted)]">
                You do not have a saved address yet. Please enter a new shipping address below.
              </p>
            )}

            {hasSavedShippingAddress && shippingAddressMode === "SAVED" ? (
              <div className="rounded-xl border border-[var(--color-border)] bg-black/30 p-3 text-sm text-slate-200">
                {savedShippingAddress}
              </div>
            ) : null}

            {!hasSavedShippingAddress || shippingAddressMode === "NEW" ? (
              <label className="block space-y-1">
                <span className="text-sm text-slate-700">New Shipping Address</span>
                <textarea
                  value={newShippingAddress}
                  onChange={(event) => setNewShippingAddress(event.target.value)}
                  className="ui-input w-full px-3 py-2 text-sm"
                  rows={3}
                  required={shippingAddressMode === "NEW" || !hasSavedShippingAddress}
                />
              </label>
            ) : null}
          </div>

          <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-4">
            <h2 className="text-lg font-semibold text-slate-900">Payment</h2>
            <div className="rounded-xl border border-[var(--color-border)] bg-black/30 px-3 py-2">
              <PaymentMethodBadge method={paymentMethod} label={currentPaymentLabel} />
            </div>

            {savedPaymentMethods.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  className={[
                    "rounded-lg border px-3 py-1.5 text-sm",
                    paymentSelectionMode === "SAVED"
                      ? "border-white/40 bg-white/15 text-white"
                      : "border-[var(--color-border)] bg-transparent text-[var(--color-text-muted)] hover:border-[var(--color-border-2)]",
                  ].join(" ")}
                  onClick={switchToSavedPayment}
                >
                  Saved payment methods
                </button>
                <button
                  type="button"
                  className={[
                    "rounded-lg border px-3 py-1.5 text-sm",
                    paymentSelectionMode === "OTHER"
                      ? "border-white/40 bg-white/15 text-white"
                      : "border-[var(--color-border)] bg-transparent text-[var(--color-text-muted)] hover:border-[var(--color-border-2)]",
                  ].join(" ")}
                  onClick={switchToOtherPayment}
                >
                  Other payment methods
                </button>
              </div>
            ) : null}

            {paymentSelectionMode === "SAVED" && savedPaymentMethods.length > 0 ? (
              <div className="space-y-2">
                {savedPaymentMethods.map((method) => {
                  const active = method.id === selectedSavedPaymentId;
                  return (
                    <button
                      key={method.id}
                      type="button"
                      onClick={() => onSelectSavedPayment(method)}
                      className={[
                        "flex w-full items-center justify-between gap-2 rounded-xl border px-3 py-2 text-left transition-colors",
                        active
                          ? "border-white/40 bg-white/10"
                          : "border-[var(--color-border)] bg-black/30 hover:border-[var(--color-border-2)]",
                      ].join(" ")}
                    >
                      <span className="flex items-center gap-2">
                        <PaymentMethodBadge method={method.type} label={method.displayName} textClassName="font-medium" />
                        {method.maskedDetail ? <span className="text-xs text-[var(--color-text-muted)]">({method.maskedDetail})</span> : null}
                      </span>
                      <span className="text-xs text-[var(--color-text-muted)]">
                        {method.isDefault ? "Default" : active ? "Current" : ""}
                      </span>
                    </button>
                  );
                })}
              </div>
            ) : (
              <FilterDropdown
                label="Payment Method"
                options={savedPaymentMethods.length > 0 ? otherPaymentMethodOptions : PAYMENT_METHOD_OPTIONS}
                value={paymentMethod}
                onChange={(nextValue) => {
                  setPaymentSelectionMode("OTHER");
                  setSelectedSavedPaymentId(null);
                  setPaymentMethod(nextValue as PaymentMethodType);
                }}
              />
            )}

            {paymentMethod === "BANK_TRANSFER" ? (
              <label className="block space-y-1">
                <span className="text-sm text-slate-700">Bank Account Detail</span>
                <input
                  value={paymentDetail}
                  onChange={(event) => setPaymentDetail(event.target.value)}
                  className="ui-input w-full px-3 py-2 text-sm"
                  required
                />
              </label>
            ) : null}

            <FilterDropdown
              label="Payment Plan"
              options={PAYMENT_PLAN_OPTIONS}
              value={paymentPlan}
              onChange={(nextValue) => setPaymentPlan(nextValue as PaymentPlanType)}
              disabled={isInstallmentDisabled}
            />

            {paymentPlan === "INSTALLMENT" ? (
              <FilterDropdown
                label="Installment Months"
                options={INSTALLMENT_MONTH_OPTIONS}
                value={String(installmentMonths)}
                onChange={(nextValue) => setInstallmentMonths(Number.parseInt(nextValue, 10))}
              />
            ) : null}

            {isInstallmentSelected ? (
              <div className="rounded-xl border border-amber-300/35 bg-amber-500/12 p-3">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs font-semibold uppercase tracking-wide text-amber-200">Installment Active</p>
                  <span className="rounded-full border border-amber-300/45 bg-amber-400/15 px-2 py-0.5 text-[11px] font-semibold text-amber-100">
                    {installmentMonths} months
                  </span>
                </div>
                <p className="mt-2 text-sm text-amber-100">
                  Estimated monthly payment:{" "}
                  <span className="font-semibold text-amber-50">
                    {installmentMonthly !== null ? formatPriceVnd(installmentMonthly) : "-"}
                  </span>
                </p>
              </div>
            ) : (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 shadow-[0_8px_20px_rgba(16,185,129,0.22)]">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">One-Time Payment Active</p>
                  <span className="rounded-full border border-emerald-300 bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                    single charge
                  </span>
                </div>
                <p className="mt-2 text-sm text-emerald-800">Full amount will be charged in a single payment.</p>
              </div>
            )}
          </div>
        </section>

        <section className="glass-panel space-y-4 rounded-3xl p-6 lg:sticky lg:top-24 lg:self-start">
          <h2 className="text-lg font-semibold text-slate-900">Order Summary</h2>
          <div className="max-h-60 space-y-2 overflow-y-auto pr-1">
            {cart.items.map((item) => (
              <div key={item.id} className="flex items-center justify-between text-sm">
                <span className="text-slate-700">
                  {item.name} x {item.quantity}
                </span>
                <span className="font-medium text-slate-900">{formatPriceVnd(item.lineTotal)}</span>
              </div>
            ))}
          </div>

          <div className="border-t border-[var(--color-border)] pt-3">
            <p className="text-sm text-slate-600">Items: {cart.itemCount}</p>
            <p className="mt-1 text-xl font-bold text-slate-900">Total: {formatPriceVnd(cart.totalAmount)}</p>
            <div
              className={[
                "mt-3 rounded-xl border p-3",
                isInstallmentSelected
                  ? "border-amber-300/35 bg-amber-500/12"
                  : "border-emerald-200 bg-emerald-50 shadow-[0_8px_20px_rgba(16,185,129,0.22)]",
              ].join(" ")}
            >
              <div className="flex items-center justify-between gap-2">
                <p className={isInstallmentSelected ? "text-xs font-semibold uppercase tracking-wide text-amber-200" : "text-xs font-semibold uppercase tracking-wide text-emerald-700"}>
                  {paymentPlanLabel}
                </p>
                <span
                  className={[
                    "rounded-full px-2 py-0.5 text-[11px] font-semibold",
                    isInstallmentSelected
                      ? "border border-amber-300/45 bg-amber-400/15 text-amber-100"
                      : "border border-emerald-300 bg-emerald-100 text-emerald-700",
                  ].join(" ")}
                >
                  {isInstallmentSelected ? `${installmentMonths} months` : "single charge"}
                </span>
              </div>

              {isInstallmentSelected ? (
                <p className="mt-2 text-sm text-amber-100">
                  Estimated monthly payment:{" "}
                  <span className="font-semibold text-amber-50">
                    {installmentMonthly !== null ? formatPriceVnd(installmentMonthly) : "-"}
                  </span>
                </p>
              ) : (
                <p className="mt-2 text-sm text-emerald-800">No monthly schedule. Charged in full at checkout.</p>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-[var(--color-border)] bg-black/30 p-3 text-xs text-[var(--color-text-muted)]">
            <p className="font-semibold text-slate-200">Shipping to</p>
            <p className="mt-1 whitespace-pre-wrap break-words text-sm text-slate-300">
              {resolvedShippingAddress || "No shipping address selected yet."}
            </p>
          </div>

          <div className="rounded-xl border border-[var(--color-border)] bg-black/30 p-3 text-xs text-slate-300">
            <p className="font-semibold text-slate-200">Current payment method</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <PaymentMethodBadge
                method={paymentMethod}
                label={currentPaymentLabel ?? currentPaymentTypeLabel}
                textClassName="text-sm font-semibold"
              />
              {paymentSelectionMode === "SAVED" && selectedSavedPayment?.maskedDetail ? (
                <span className="text-xs text-[var(--color-text-muted)]">({selectedSavedPayment.maskedDetail})</span>
              ) : null}
              {paymentSelectionMode === "SAVED" && selectedSavedPayment?.isDefault ? (
                <span className="text-xs text-[var(--color-text-muted)]">- default saved method</span>
              ) : paymentSelectionMode === "SAVED" ? (
                <span className="text-xs text-[var(--color-text-muted)]">- saved method</span>
              ) : (
                <span className="text-xs text-[var(--color-text-muted)]">- other method</span>
              )}
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitDisabled}
            className="ui-btn ui-btn-primary inline-flex w-full items-center justify-center gap-2 px-4 py-2.5 text-sm"
          >
            <GriddyIcon name="check" />
            {submitting ? "Placing order..." : "Place Order"}
          </button>
        </section>
      </form>
    </div>
  );
}

