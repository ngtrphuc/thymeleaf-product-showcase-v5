export type OrderStatusBadge = {
  label: string;
  className: string;
};

const STATUS_CLASS_BY_KEY: Record<string, string> = {
  pending: "border-[rgba(251,191,36,0.45)] bg-[rgba(251,191,36,0.18)] text-[#fbbf24]",
  processing: "border-[rgba(56,189,248,0.45)] bg-[rgba(56,189,248,0.18)] text-[#7dd3fc]",
  shipped: "border-[rgba(167,139,250,0.68)] bg-[rgba(167,139,250,0.32)] text-[#ede9fe]",
  delivered: "border-[rgba(52,211,153,0.45)] bg-[rgba(52,211,153,0.18)] text-[#6ee7b7]",
  cancelled: "border-[rgba(251,113,133,0.45)] bg-[rgba(251,113,133,0.18)] text-[#fda4af]",
};

function normalizeStatus(input: string | null | undefined): string {
  return (input ?? "").trim().toLowerCase();
}

export function getOrderStatusBadge(status: string | null | undefined): OrderStatusBadge {
  const normalized = normalizeStatus(status);
  if (normalized.length === 0) {
    return {
      label: "unknown",
      className: "border-[rgba(148,163,184,0.4)] bg-[rgba(148,163,184,0.15)] text-[#cbd5e1]",
    };
  }

  return {
    label: normalized,
    className:
      STATUS_CLASS_BY_KEY[normalized] ??
      "border-[rgba(148,163,184,0.4)] bg-[rgba(148,163,184,0.15)] text-[#cbd5e1]",
  };
}
