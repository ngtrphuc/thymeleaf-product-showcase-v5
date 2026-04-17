"use client";

type ErrorProps = {
  error: Error;
  reset: () => void;
};

export default function ProductsError({ error, reset }: ErrorProps) {
  return (
    <main className="glass-panel rounded-3xl p-8">
      <h1 className="text-2xl font-bold text-slate-900">Không tải được catalog</h1>
      <p className="mt-2 text-sm text-slate-600">{error.message}</p>
      <button
        type="button"
        onClick={reset}
        className="mt-5 rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--color-primary-strong)]"
      >
        Thử lại
      </button>
    </main>
  );
}
