/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  addPaymentMethod,
  ApiError,
  authLogout,
  fetchProfile,
  removePaymentMethod,
  setDefaultPaymentMethod,
  updateProfile,
  type ProfileResponse,
} from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";
import { AuthMotionIcon } from "@/components/ui/auth-motion-icon";
import { PaymentMethodBadge } from "@/components/storefront/payment-method-badge";

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [defaultAddress, setDefaultAddress] = useState("");

  const [newPaymentType, setNewPaymentType] = useState("CASH_ON_DELIVERY");
  const [newPaymentDetail, setNewPaymentDetail] = useState("");

  async function loadProfile() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchProfile();
      setProfile(data);
      setFullName(data.fullName ?? "");
      setPhoneNumber(data.phoneNumber ?? "");
      setDefaultAddress(data.defaultAddress ?? "");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load profile.");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadProfile();
  }, []);

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const updated = await updateProfile({ fullName, phoneNumber, defaultAddress });
      setProfile(updated);
      setMessage("Profile updated.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to update profile.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function addPayment() {
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const methods = await addPaymentMethod({
        type: newPaymentType,
        bankDetail: newPaymentType === "BANK_TRANSFER" ? newPaymentDetail : null,
        setAsDefault: false,
      });
      setProfile((prev) => (prev ? { ...prev, paymentMethods: methods } : prev));
      setNewPaymentDetail("");
      setMessage("Payment method added.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to add payment method.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function setDefault(id: number) {
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const methods = await setDefaultPaymentMethod(id);
      setProfile((prev) => (prev ? { ...prev, paymentMethods: methods } : prev));
      setMessage("Default payment method updated.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to update default payment method.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function removePayment(id: number) {
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const methods = await removePaymentMethod(id);
      setProfile((prev) => (prev ? { ...prev, paymentMethods: methods } : prev));
      setMessage("Payment method removed.");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to remove payment method.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function logout() {
    setSaving(true);
    setError(null);
    try {
      await authLogout();
      window.location.href = "/login?reauth=1";
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Logout failed.");
      }
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading profile...</div>;
  }

  if (!profile) {
    return <div className="glass-panel rounded-3xl p-8 text-center text-red-700">{error ?? "Profile not available."}</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Profile</h1>
            <p className="mt-1 text-sm text-slate-600">{profile.email}</p>
          </div>
          <button
            type="button"
            onClick={() => void logout()}
            disabled={saving}
            className="ui-btn ui-btn-logout inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold"
          >
            <AuthMotionIcon variant="logout" className="h-4 w-4" />
            Sign Out
          </button>
        </div>
      </header>

      {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <section className="grid gap-4 sm:grid-cols-3">
        <div className="glass-panel rounded-2xl p-4 text-center">
          <p className="text-xs uppercase text-slate-500">Delivered Orders</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{profile.deliveredOrderCount}</p>
        </div>
        <div className="glass-panel rounded-2xl p-4 text-center">
          <p className="text-xs uppercase text-slate-500">Pending Orders</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{profile.pendingOrderCount}</p>
        </div>
        <div className="glass-panel rounded-2xl p-4 text-center">
          <p className="text-xs uppercase text-slate-500">Cart Items</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{profile.cartItemCount}</p>
        </div>
      </section>

      <form onSubmit={saveProfile} className="glass-panel space-y-4 rounded-3xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">Account Details</h2>

        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Full Name</span>
          <input
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
            required
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Phone Number</span>
          <input
            value={phoneNumber}
            onChange={(event) => setPhoneNumber(event.target.value)}
            className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Default Address</span>
          <textarea
            value={defaultAddress}
            onChange={(event) => setDefaultAddress(event.target.value)}
            className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
            rows={3}
          />
        </label>

        <button
          type="submit"
          disabled={saving}
          className="inline-flex items-center gap-2 rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-black"
        >
          <GriddyIcon name="check" />
          {saving ? "Saving..." : "Save Profile"}
        </button>
      </form>

      <section className="glass-panel space-y-4 rounded-3xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">Payment Methods</h2>

        <div className="space-y-2">
          {profile.paymentMethods.length === 0 ? (
            <p className="text-sm text-slate-600">No payment method saved yet.</p>
          ) : (
            profile.paymentMethods.map((method) => (
              <article key={method.id} className="rounded-xl border border-[var(--color-border)] bg-white p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <PaymentMethodBadge
                      method={method.type}
                      label={`${method.displayName}${method.isDefault ? " (Default)" : ""}`}
                      textClassName="font-semibold text-slate-900"
                    />
                    {method.maskedDetail ? <p className="text-xs text-slate-600">{method.maskedDetail}</p> : null}
                  </div>
                  <div className="flex gap-2">
                    {!method.isDefault ? (
                      <button
                        type="button"
                        disabled={saving}
                        onClick={() => void setDefault(method.id)}
                        className="inline-flex items-center gap-1 rounded-lg border border-[var(--color-border)] bg-white px-3 py-1.5 text-xs font-semibold"
                      >
                        <GriddyIcon name="check" />
                        Set Default
                      </button>
                    ) : null}
                    <button
                      type="button"
                      disabled={saving}
                      onClick={() => void removePayment(method.id)}
                      className="inline-flex items-center gap-1 rounded-lg bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700"
                    >
                      <GriddyIcon name="trash" />
                      Remove
                    </button>
                  </div>
                </div>
              </article>
            ))
          )}
        </div>

        <div className="rounded-xl border border-[var(--color-border)] bg-white p-4">
          <h3 className="text-sm font-semibold text-slate-900">Add Payment Method</h3>
          <div className="mt-2">
            <PaymentMethodBadge method={newPaymentType} />
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            <select
              value={newPaymentType}
              onChange={(event) => setNewPaymentType(event.target.value)}
              className="ui-dropdown-native rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
            >
              <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="PAYPAY">PayPay</option>
              <option value="MASTERCARD">Credit Card</option>
            </select>
            <input
              value={newPaymentDetail}
              onChange={(event) => setNewPaymentDetail(event.target.value)}
              placeholder="Bank detail (for transfer)"
              className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm sm:col-span-2"
              disabled={newPaymentType !== "BANK_TRANSFER"}
            />
          </div>
          <button
            type="button"
            disabled={saving}
            onClick={() => void addPayment()}
            className="mt-3 inline-flex items-center gap-2 rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-black"
          >
            <GriddyIcon name="credit-card" />
            Add Method
          </button>
        </div>
      </section>
    </div>
  );
}

