import Link from "next/link";

export default function ProductNotFound() {
  return (
    <main className="glass-panel rounded-3xl p-8 text-center">
      <h1 className="text-2xl font-bold text-slate-900">Không tìm thấy sản phẩm</h1>
      <p className="mt-2 text-sm text-slate-600">
        Sản phẩm có thể đã bị xoá hoặc chưa tồn tại trong cơ sở dữ liệu.
      </p>
      <Link
        href="/products"
        className="mt-5 inline-flex rounded-xl bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--color-primary-strong)]"
      >
        Quay về catalog
      </Link>
    </main>
  );
}
