import Image from "next/image";

type PaymentMethodKey = "CASH_ON_DELIVERY" | "BANK_TRANSFER" | "PAYPAY" | "MASTERCARD" | "UNKNOWN";

type PaymentMethodMeta = {
  key: PaymentMethodKey;
  label: string;
  iconSrc: string;
};

const METHOD_META: Record<Exclude<PaymentMethodKey, "UNKNOWN">, PaymentMethodMeta> = {
  CASH_ON_DELIVERY: {
    key: "CASH_ON_DELIVERY",
    label: "Cash on Delivery",
    iconSrc: "/payments/cash.svg",
  },
  BANK_TRANSFER: {
    key: "BANK_TRANSFER",
    label: "Bank Transfer",
    iconSrc: "/payments/bank.svg",
  },
  PAYPAY: {
    key: "PAYPAY",
    label: "PayPay",
    iconSrc: "/payments/paypay.png",
  },
  MASTERCARD: {
    key: "MASTERCARD",
    label: "Credit Card",
    iconSrc: "/payments/creditcard.svg",
  },
};

const FALLBACK_METHOD: PaymentMethodMeta = {
  key: "UNKNOWN",
  label: "Payment",
  iconSrc: "/payments/payment-generic.svg",
};

function joinClassName(...parts: Array<string | undefined>): string {
  return parts.filter(Boolean).join(" ");
}

function normalizePaymentMethod(input: string | null | undefined): string {
  return (input ?? "")
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_")
    .replace(/-/g, "_");
}

export function resolvePaymentMethodMeta(input: string | null | undefined): PaymentMethodMeta {
  const normalized = normalizePaymentMethod(input);

  if (normalized in METHOD_META) {
    return METHOD_META[normalized as keyof typeof METHOD_META];
  }

  if (normalized.includes("CASH") || normalized.includes("DELIVERY") || normalized.includes("COD")) {
    return METHOD_META.CASH_ON_DELIVERY;
  }
  if (normalized.includes("BANK") || normalized.includes("TRANSFER")) {
    return METHOD_META.BANK_TRANSFER;
  }
  if (normalized.includes("PAYPAY")) {
    return METHOD_META.PAYPAY;
  }
  if (normalized.includes("MASTER") || normalized.includes("CARD")) {
    return METHOD_META.MASTERCARD;
  }

  return FALLBACK_METHOD;
}

type PaymentMethodBadgeProps = {
  method: string | null | undefined;
  label?: string | null;
  className?: string;
  iconClassName?: string;
  textClassName?: string;
};

export function PaymentMethodBadge({
  method,
  label,
  className,
  iconClassName,
  textClassName,
}: PaymentMethodBadgeProps) {
  const meta = resolvePaymentMethodMeta(method);
  const display = label && label.trim().length > 0 ? label : meta.label;

  return (
    <span className={joinClassName("inline-flex items-center gap-2", className)}>
      <span
        className={joinClassName(
          "relative h-8 w-11 shrink-0 overflow-hidden rounded border border-[var(--color-border)] bg-transparent",
          iconClassName,
        )}
      >
        <Image
          src={meta.iconSrc}
          alt={display}
          fill
          sizes="56px"
          className="object-contain"
        />
      </span>
      <span className={joinClassName("text-sm text-slate-800", textClassName)}>{display}</span>
    </span>
  );
}


