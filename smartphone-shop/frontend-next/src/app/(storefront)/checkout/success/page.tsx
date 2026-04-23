"use client";

import Link from "next/link";
import { useMemo } from "react";
import { useSearchParams } from "next/navigation";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function CheckoutSuccessPage() {
  const searchParams = useSearchParams();
  const orderCode = useMemo(() => {
    const code = searchParams.get("code")?.trim() ?? "";
    return code.length > 0 ? code : null;
  }, [searchParams]);

  return (
    <div className="glass-panel rounded-3xl p-8 text-center">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
        <GriddyIcon name="check" className="h-7 w-7" />
      </div>
      <h1 className="mt-4 text-2xl font-bold text-slate-900">Order placed successfully</h1>
      <p className="mt-2 text-sm text-slate-700">
        {orderCode ? (
          <>
            Your order code is <span className="font-semibold text-slate-900">{orderCode}</span>.
          </>
        ) : (
          <>Your order was created successfully.</>
        )}
      </p>
      <p className="mt-1 text-sm text-slate-600">You can track the order status in My Orders.</p>

      <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
        <Link
          href="/orders"
          className="ui-btn ui-btn-primary inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm"
        >
          <GriddyIcon name="orders" />
          View My Orders
        </Link>
        <Link
          href="/products"
          className="inline-flex items-center gap-2 rounded-xl border border-[var(--color-border)] bg-white px-4 py-2 text-sm font-semibold text-slate-700"
        >
          Continue Shopping
        </Link>
      </div>
    </div>
  );
}

