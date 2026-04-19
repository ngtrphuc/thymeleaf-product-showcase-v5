/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
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

type PaymentMethodType = "CASH_ON_DELIVERY" | "BANK_TRANSFER" | "PAYPAY" | "MASTERCARD";
type PaymentPlanType = "FULL_PAYMENT" | "INSTALLMENT";

export default function CheckoutPage() {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [customerName, setCustomerName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [shippingAddress, setShippingAddress] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodType>("CASH_ON_DELIVERY");
  const [paymentDetail, setPaymentDetail] = useState("");
  const [paymentPlan, setPaymentPlan] = useState<PaymentPlanType>("FULL_PAYMENT");
  const [installmentMonths, setInstallmentMonths] = useState<number>(24);

  async function loadCheckoutData() {
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
      setShippingAddress(profileData.defaultAddress ?? "");

      const defaultMethod = paymentData.find((method) => method.isDefault);
      if (defaultMethod) {
        const methodType = defaultMethod.type as PaymentMethodType;
        if (["CASH_ON_DELIVERY", "BANK_TRANSFER", "PAYPAY", "MASTERCARD"].includes(methodType)) {
          setPaymentMethod(methodType);
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
  }

  useEffect(() => {
    void loadCheckoutData();
  }, []);

  const isInstallmentDisabled = paymentMethod === "CASH_ON_DELIVERY";

  useEffect(() => {
    if (isInstallmentDisabled) {
      setPaymentPlan("FULL_PAYMENT");
    }
  }, [isInstallmentDisabled]);

  const installmentMonthly = useMemo(() => {
    if (!cart || paymentPlan !== "INSTALLMENT") {
      return null;
    }
    return Math.round((cart.totalAmount || 0) / installmentMonths);
  }, [cart, paymentPlan, installmentMonths]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const created = await placeOrder({
        customerName,
        phoneNumber,
        shippingAddress,
        paymentMethod,
        paymentDetail: paymentMethod === "BANK_TRANSFER" ? paymentDetail : null,
        paymentPlan,
        installmentMonths: paymentPlan === "INSTALLMENT" ? installmentMonths : null,
      });
      setSuccess(`Order ${created.orderCode} placed successfully.`);
      await loadCheckoutData();
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

      {error ? <p className="text-sm text-red-700">{error}</p> : null}
      {success ? <p className="text-sm text-emerald-700">{success}</p> : null}

      <form className="grid gap-6 lg:grid-cols-2" onSubmit={onSubmit}>
        <section className="glass-panel space-y-4 rounded-3xl p-6">
          <h2 className="text-lg font-semibold text-slate-900">Shipping Details</h2>

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

          <label className="block space-y-1">
            <span className="text-sm text-slate-700">Shipping Address</span>
            <textarea
              value={shippingAddress}
              onChange={(event) => setShippingAddress(event.target.value)}
              className="ui-input w-full px-3 py-2 text-sm"
              rows={3}
              required
            />
          </label>

          <h2 className="pt-2 text-lg font-semibold text-slate-900">Payment</h2>
          <div className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2">
            <PaymentMethodBadge method={paymentMethod} />
          </div>

          <label className="block space-y-1">
            <span className="text-sm text-slate-700">Payment Method</span>
            <select
              value={paymentMethod}
              onChange={(event) => setPaymentMethod(event.target.value as PaymentMethodType)}
              className="ui-input w-full px-3 py-2 text-sm"
            >
              <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="PAYPAY">PayPay</option>
              <option value="MASTERCARD">Credit Card</option>
            </select>
          </label>

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

          <label className="block space-y-1">
            <span className="text-sm text-slate-700">Payment Plan</span>
            <select
              value={paymentPlan}
              onChange={(event) => setPaymentPlan(event.target.value as PaymentPlanType)}
              className="ui-input w-full px-3 py-2 text-sm"
              disabled={isInstallmentDisabled}
            >
              <option value="FULL_PAYMENT">Full Payment</option>
              <option value="INSTALLMENT">Installment</option>
            </select>
          </label>

          {paymentPlan === "INSTALLMENT" ? (
            <label className="block space-y-1">
              <span className="text-sm text-slate-700">Installment Months</span>
              <select
                value={installmentMonths}
                onChange={(event) => setInstallmentMonths(Number.parseInt(event.target.value, 10))}
                className="ui-input w-full px-3 py-2 text-sm"
              >
                <option value={6}>6 months</option>
                <option value={12}>12 months</option>
                <option value={24}>24 months</option>
              </select>
            </label>
          ) : null}
        </section>

        <section className="glass-panel space-y-4 rounded-3xl p-6">
          <h2 className="text-lg font-semibold text-slate-900">Order Summary</h2>
          <div className="space-y-2">
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
            {installmentMonthly !== null ? (
              <p className="mt-1 text-sm text-slate-600">
                Estimated monthly installment: {formatPriceVnd(installmentMonthly)}
              </p>
            ) : null}
          </div>

          {paymentMethods.length > 0 ? (
            <div className="rounded-xl border border-[var(--color-border)] bg-white p-3 text-xs text-slate-600">
              <p className="font-semibold text-slate-800">Saved payment methods</p>
              <ul className="mt-2 space-y-1">
                {paymentMethods.map((method) => (
                  <li key={method.id} className="flex flex-wrap items-center gap-2">
                    <PaymentMethodBadge
                      method={method.type}
                      label={method.displayName}
                      textClassName="text-xs text-slate-700"
                    />
                    {method.maskedDetail ? <span>({method.maskedDetail})</span> : null}
                    {method.isDefault ? <span>- default</span> : null}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <button
            type="submit"
            disabled={submitting}
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

