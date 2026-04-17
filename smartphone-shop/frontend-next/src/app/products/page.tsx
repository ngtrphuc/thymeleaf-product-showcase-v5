import Link from "next/link";
import { fetchCatalogPage } from "@/lib/api";
import { ProductCard } from "@/components/storefront/product-card";

type SearchValue = string | string[] | undefined;

type ProductsPageProps = {
  searchParams?: Promise<Record<string, SearchValue>> | Record<string, SearchValue>;
};

const SORT_OPTIONS = [
  { label: "Tên A-Z", value: "name_asc" },
  { label: "Tên Z-A", value: "name_desc" },
  { label: "Giá tăng dần", value: "price_asc" },
  { label: "Giá giảm dần", value: "price_desc" },
];

function readFirst(value: SearchValue): string | undefined {
  if (Array.isArray(value)) {
    return value[0];
  }
  return value;
}

function positiveInt(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return fallback;
  }
  return parsed;
}

export default async function ProductsPage({ searchParams }: ProductsPageProps) {
  const resolved = await Promise.resolve(searchParams ?? {});
  const keyword = readFirst(resolved.keyword)?.trim() ?? "";
  const brand = readFirst(resolved.brand)?.trim() ?? "";
  const sort = readFirst(resolved.sort)?.trim() ?? "name_asc";
  const page = positiveInt(readFirst(resolved.page), 0);
  const pageSize = readFirst(resolved.pageSize) === "8" ? "8" : "9";

  const query = new URLSearchParams();
  if (keyword.length > 0) query.set("keyword", keyword);
  if (brand.length > 0) query.set("brand", brand);
  if (sort.length > 0) query.set("sort", sort);
  query.set("page", String(page));
  query.set("pageSize", pageSize);

  const catalog = await fetchCatalogPage(query);

  const currentPage = Math.max(0, catalog.currentPage);
  const totalPages = Math.max(1, catalog.totalPages);
  const previousPage = Math.max(currentPage - 1, 0);
  const nextPage = Math.min(currentPage + 1, totalPages - 1);

  const pageHref = (targetPage: number) => {
    const nextQuery = new URLSearchParams(query);
    nextQuery.set("page", String(targetPage));
    const serialized = nextQuery.toString();
    return `/products${serialized.length > 0 ? `?${serialized}` : ""}`;
  };

  return (
    <main className="space-y-6">
      <header className="glass-panel rounded-3xl p-6 sm:p-8">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
          Smartphone Shop v5
        </p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900 sm:text-4xl">
          Product Catalog
        </h1>
        <p className="mt-3 max-w-2xl text-sm text-[var(--color-text-muted)] sm:text-base">
          Frontend Next.js App Router đã kết nối trực tiếp tới Spring Boot REST
          API. Đây là điểm bắt đầu chính thức cho Phase 2.
        </p>
      </header>

      <section className="glass-panel rounded-3xl p-5">
        <form className="grid gap-4 md:grid-cols-4">
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-slate-700">Từ khóa</span>
            <input
              name="keyword"
              defaultValue={keyword}
              placeholder="Ví dụ: iPhone, Samsung..."
              className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm outline-none ring-[var(--color-primary)] focus:ring-2"
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-slate-700">Thương hiệu</span>
            <select
              name="brand"
              defaultValue={brand}
              className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm outline-none ring-[var(--color-primary)] focus:ring-2"
            >
              <option value="">Tất cả thương hiệu</option>
              {catalog.brands.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-slate-700">Sắp xếp</span>
            <select
              name="sort"
              defaultValue={sort}
              className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm outline-none ring-[var(--color-primary)] focus:ring-2"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-slate-700">Số item/trang</span>
            <select
              name="pageSize"
              defaultValue={pageSize}
              className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm outline-none ring-[var(--color-primary)] focus:ring-2"
            >
              <option value="9">9 (desktop)</option>
              <option value="8">8 (compact)</option>
            </select>
          </label>

          <div className="md:col-span-4">
            <button
              type="submit"
              className="inline-flex items-center rounded-xl bg-[var(--color-primary)] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[var(--color-primary-strong)]"
            >
              Lọc sản phẩm
            </button>
          </div>
        </form>
      </section>

      <section className="flex items-center justify-between rounded-2xl px-2 text-sm text-slate-600">
        <p>
          Hiển thị <strong>{catalog.products.length}</strong> /{" "}
          <strong>{catalog.totalElements}</strong> sản phẩm
        </p>
        <p>
          Page <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong>
        </p>
      </section>

      {catalog.products.length === 0 ? (
        <section className="glass-panel rounded-3xl p-8 text-center">
          <h2 className="text-xl font-semibold text-slate-900">
            Không tìm thấy sản phẩm phù hợp
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Thử xoá bớt điều kiện lọc để xem thêm kết quả.
          </p>
        </section>
      ) : (
        <section className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {catalog.products.map((product) => (
            <ProductCard key={product.id ?? `${product.name}-${product.brand}`} product={product} />
          ))}
        </section>
      )}

      <nav className="glass-panel flex items-center justify-between rounded-2xl p-4">
        <Link
          href={pageHref(previousPage)}
          className={`rounded-lg px-4 py-2 text-sm font-medium ${
            currentPage === 0
              ? "pointer-events-none bg-slate-100 text-slate-400"
              : "bg-white text-slate-700 hover:bg-slate-100"
          }`}
        >
          Trang trước
        </Link>

        <div className="hidden gap-2 sm:flex">
          {Array.from({ length: totalPages }).map((_, index) => (
            <Link
              key={index}
              href={pageHref(index)}
              className={`rounded-lg px-3 py-1.5 text-sm ${
                index === currentPage
                  ? "bg-[var(--color-primary)] font-semibold text-white"
                  : "bg-white text-slate-700 hover:bg-slate-100"
              }`}
            >
              {index + 1}
            </Link>
          ))}
        </div>

        <Link
          href={pageHref(nextPage)}
          className={`rounded-lg px-4 py-2 text-sm font-medium ${
            currentPage >= totalPages - 1
              ? "pointer-events-none bg-slate-100 text-slate-400"
              : "bg-white text-slate-700 hover:bg-slate-100"
          }`}
        >
          Trang sau
        </Link>
      </nav>
    </main>
  );
}
