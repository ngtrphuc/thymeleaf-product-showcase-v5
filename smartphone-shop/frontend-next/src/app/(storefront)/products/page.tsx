import Link from "next/link";
import { fetchCatalogPage } from "@/lib/api";
import { ProductCard } from "@/components/storefront/product-card";
import { CatalogFilters } from "@/components/storefront/catalog-filters";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type SearchValue = string | string[] | undefined;

type ProductsPageProps = {
  searchParams?: Promise<Record<string, SearchValue>>;
};

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
  const resolved: Record<string, SearchValue> = await (
    searchParams ?? Promise.resolve({} as Record<string, SearchValue>)
  );

  const keyword = readFirst(resolved.keyword)?.trim() ?? "";
  const brand = readFirst(resolved.brand)?.trim() ?? "";
  const sort = readFirst(resolved.sort)?.trim() ?? "name_asc";
  const storage = readFirst(resolved.storage)?.trim() ?? "";
  const priceRange = readFirst(resolved.priceRange)?.trim() ?? "";
  const priceMin = readFirst(resolved.priceMin)?.trim() ?? "";
  const priceMax = readFirst(resolved.priceMax)?.trim() ?? "";
  const batteryRange = readFirst(resolved.batteryRange)?.trim() ?? "";
  const batteryMin = readFirst(resolved.batteryMin)?.trim() ?? "";
  const batteryMax = readFirst(resolved.batteryMax)?.trim() ?? "";
  const screenSize = readFirst(resolved.screenSize)?.trim() ?? "";
  const page = positiveInt(readFirst(resolved.page), 0);

  const query = new URLSearchParams();
  if (keyword.length > 0) query.set("keyword", keyword);
  if (brand.length > 0) query.set("brand", brand);
  if (sort.length > 0) query.set("sort", sort);
  if (storage.length > 0) query.set("storage", storage);
  if (priceRange.length > 0) query.set("priceRange", priceRange);
  if (priceMin.length > 0) query.set("priceMin", priceMin);
  if (priceMax.length > 0) query.set("priceMax", priceMax);
  if (batteryRange.length > 0) query.set("batteryRange", batteryRange);
  if (batteryMin.length > 0) query.set("batteryMin", batteryMin);
  if (batteryMax.length > 0) query.set("batteryMax", batteryMax);
  if (screenSize.length > 0) query.set("screenSize", screenSize);
  query.set("page", String(page));

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
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6 sm:p-8">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
          Migration Track
        </p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900 sm:text-4xl">Product Catalog</h1>
        <p className="mt-3 max-w-2xl text-sm text-[var(--color-text-muted)] sm:text-base">
          This page reads directly from Spring Boot REST APIs and is now part of the
          new Next.js storefront journey.
        </p>
      </header>

      <CatalogFilters
        brands={catalog.brands}
        initialValues={{
          keyword,
          brand,
          sort,
          storage,
          priceRange,
          priceMin,
          priceMax,
          batteryRange,
          batteryMin,
          batteryMax,
          screenSize,
        }}
      />

      <section className="flex items-center justify-between rounded-2xl px-2 text-sm text-slate-600">
        <p>
          Showing <strong>{catalog.products.length}</strong> / <strong>{catalog.totalElements}</strong>
        </p>
        <p>
          Page <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong>
        </p>
      </section>

      {catalog.products.length === 0 ? (
        <section className="glass-panel rounded-3xl p-8 text-center">
          <h2 className="text-xl font-semibold text-slate-900">No products found</h2>
          <p className="mt-2 text-sm text-slate-600">Try removing a filter or broadening your keyword.</p>
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
          className={`ui-btn inline-flex items-center gap-2 px-4 py-2 text-sm ${
            currentPage === 0
              ? "pointer-events-none border border-[var(--color-border)] bg-slate-100 text-slate-400"
              : "ui-btn-secondary"
          }`}
        >
          <GriddyIcon name="arrow-left" />
          Previous
        </Link>

        <div className="hidden gap-2 sm:flex">
          {Array.from({ length: totalPages }).map((_, index) => (
            <Link
              key={index}
              href={pageHref(index)}
              className={`ui-btn px-3 py-1.5 text-sm ${
                index === currentPage
                  ? "ui-btn-primary"
                  : "ui-btn-secondary"
              }`}
            >
              {index + 1}
            </Link>
          ))}
        </div>

        <Link
          href={pageHref(nextPage)}
          className={`ui-btn inline-flex items-center gap-2 px-4 py-2 text-sm ${
            currentPage >= totalPages - 1
              ? "pointer-events-none border border-[var(--color-border)] bg-slate-100 text-slate-400"
              : "ui-btn-secondary"
          }`}
        >
          Next
          <GriddyIcon name="arrow-right" />
        </Link>
      </nav>
    </div>
  );
}
