import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, fetchProductDetail, toAssetUrl, type ProductSummary } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { ProductActions } from "@/components/storefront/product-actions";

type ProductDetailPageProps = {
  params: Promise<{ id: string }> | { id: string };
};

function AvailabilityBadge({ product }: { product: ProductSummary }) {
  return (
    <span
      className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${
        product.available ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"
      }`}
    >
      {product.availabilityLabel || "Unavailable"}
    </span>
  );
}

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { id } = await Promise.resolve(params);

  let detail;
  try {
    detail = await fetchProductDetail(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }

  const product = detail.product;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <Link href="/products" className="hover:text-[var(--color-primary)]">
          Catalog
        </Link>
        <span>/</span>
        <span className="font-medium text-slate-900">{product.name}</span>
      </div>

      <section className="glass-panel grid gap-6 rounded-3xl p-5 md:grid-cols-2 md:p-8">
        <div className="overflow-hidden rounded-3xl bg-[var(--color-surface-soft)]">
          <Image
            src={toAssetUrl(product.imageUrl)}
            alt={product.name}
            width={860}
            height={860}
            className="h-full w-full object-cover"
            unoptimized
          />
        </div>

        <div className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{product.brand}</p>
          <h1 className="text-3xl font-bold text-slate-900">{product.name}</h1>
          <AvailabilityBadge product={product} />
          <p className="text-3xl font-bold text-[var(--color-primary-strong)]">{formatPriceVnd(product.price)}</p>
          <div className="rounded-2xl border border-[var(--color-border)] bg-white p-4">
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-slate-500">Storage</dt>
                <dd className="font-semibold text-slate-900">{product.storage || "N/A"}</dd>
              </div>
              <div>
                <dt className="text-slate-500">RAM</dt>
                <dd className="font-semibold text-slate-900">{product.ram || "N/A"}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Screen</dt>
                <dd className="font-semibold text-slate-900">{product.size || "N/A"}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Stock</dt>
                <dd className="font-semibold text-slate-900">{product.stock ?? 0}</dd>
              </div>
            </dl>
          </div>
          <p className="text-sm text-slate-600">
            Estimated installment from <strong>{formatPriceVnd(product.monthlyInstallmentAmount)}</strong> / month.
          </p>
          {product.id ? <ProductActions productId={product.id} /> : null}
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-bold text-slate-900">Recommended Products</h2>
        {detail.recommendedProducts.length === 0 ? (
          <div className="glass-panel rounded-2xl p-5 text-sm text-slate-600">
            No recommendations are available right now.
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {detail.recommendedProducts.map((recommended) => (
              <Link
                key={recommended.id ?? `${recommended.name}-${recommended.brand}`}
                href={`/products/${recommended.id ?? ""}`}
                className="glass-panel rounded-2xl p-3 hover:-translate-y-0.5"
              >
                <Image
                  src={toAssetUrl(recommended.imageUrl)}
                  alt={recommended.name}
                  width={480}
                  height={480}
                  className="aspect-square w-full rounded-xl object-cover"
                  unoptimized
                />
                <p className="mt-2 text-sm font-semibold text-slate-900">{recommended.name}</p>
                <p className="mt-1 text-sm font-bold text-[var(--color-primary-strong)]">
                  {formatPriceVnd(recommended.price)}
                </p>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
