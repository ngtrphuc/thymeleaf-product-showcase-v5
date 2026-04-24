import { fetchCatalogPage } from "@/lib/api";
import { CatalogFilters } from "@/components/storefront/catalog-filters";
import { CatalogPagedGrid } from "@/components/storefront/catalog-paged-grid";
import { CatalogViewportSync } from "@/components/storefront/catalog-viewport-sync";

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

function resolvePageSize(value: string | undefined): number {
  return value === "8" ? 8 : 9;
}

function resolveDirection(value: string | undefined): "forward" | "backward" {
  return value === "backward" ? "backward" : "forward";
}

export default async function ProductsPage({ searchParams }: ProductsPageProps) {
  const resolved: Record<string, SearchValue> = await (
    searchParams ?? Promise.resolve({} as Record<string, SearchValue>)
  );

  const keyword = readFirst(resolved.keyword)?.trim() ?? "";
  const brand = readFirst(resolved.brand)?.trim() ?? "";
  const sort = readFirst(resolved.sort)?.trim() ?? (keyword ? "relevance" : "name_asc");
  const storage = readFirst(resolved.storage)?.trim() ?? "";
  const priceRange = readFirst(resolved.priceRange)?.trim() ?? "";
  const priceMin = readFirst(resolved.priceMin)?.trim() ?? "";
  const priceMax = readFirst(resolved.priceMax)?.trim() ?? "";
  const batteryRange = readFirst(resolved.batteryRange)?.trim() ?? "";
  const screenSize = readFirst(resolved.screenSize)?.trim() ?? "";
  const direction = resolveDirection(readFirst(resolved.dir)?.trim());
  const page = positiveInt(readFirst(resolved.page), 0);
  const pageSize = resolvePageSize(readFirst(resolved.pageSize)?.trim());

  const query = new URLSearchParams();
  if (keyword.length > 0) query.set("keyword", keyword);
  if (brand.length > 0) query.set("brand", brand);
  if (sort.length > 0) query.set("sort", sort);
  if (storage.length > 0) query.set("storage", storage);
  if (priceRange.length > 0) query.set("priceRange", priceRange);
  if (priceMin.length > 0) query.set("priceMin", priceMin);
  if (priceMax.length > 0) query.set("priceMax", priceMax);
  if (batteryRange.length > 0) query.set("batteryRange", batteryRange);
  if (screenSize.length > 0) query.set("screenSize", screenSize);
  query.set("page", String(page));
  query.set("pageSize", String(pageSize));

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
      <CatalogViewportSync currentPageSize={catalog.pageSize} />

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
          screenSize,
        }}
      />

      <section className="flex items-center justify-between rounded-2xl px-2 text-sm text-slate-600">
        <p>
          Showing <strong>{catalog.products.length}</strong> / <strong>{catalog.totalElements}</strong>
        </p>
        <p>
          Page <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong> | <strong>{catalog.pageSize}</strong>{" "}
          items per page
        </p>
      </section>

      {catalog.products.length === 0 ? (
        <section className="glass-panel rounded-3xl p-8 text-center">
          <h2 className="text-xl font-semibold text-slate-900">No products found</h2>
          <p className="mt-2 text-sm text-slate-600">Try removing a filter or broadening your keyword.</p>
        </section>
      ) : (
        <CatalogPagedGrid
          key={`${pageSize}:${currentPage}:${catalog.products.map((product) => product.id ?? product.name).join("|")}`}
          initialDirection={direction}
          products={catalog.products}
          paginationItems={[
            {
              href: pageHref(previousPage),
              label: "Previous",
              disabled: currentPage === 0,
              icon: "arrow-left",
            },
            ...Array.from({ length: totalPages }).map((_, index) => ({
              href: pageHref(index),
              label: String(index + 1),
              active: index === currentPage,
            })),
            {
              href: pageHref(nextPage),
              label: "Next",
              disabled: currentPage >= totalPages - 1,
              icon: "arrow-right" as const,
              iconTrailing: true,
            },
          ]}
        />
      )}
    </div>
  );
}
