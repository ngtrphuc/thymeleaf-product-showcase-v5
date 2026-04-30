import Image from "next/image";
import Link from "next/link";
import type { ProductSummary } from "@/lib/api";
import { toAssetUrl } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { QuickProductActions } from "@/components/storefront/quick-product-actions";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type ProductCardProps = {
  product: ProductSummary;
  motionReduced?: boolean;
};

export function ProductCard({ product, motionReduced = false }: ProductCardProps) {
  const productId = product.id ?? 0;
  const productHref = product.id ? `/products/${productId}` : null;
  const mediaClassName = motionReduced
    ? "h-full w-full object-contain p-2"
    : "h-full w-full object-contain p-2 transition-transform duration-150 ease-out group-hover:-translate-y-px";

  return (
    <article
      className="glass-panel card-hover group overflow-hidden rounded-3xl"
      data-motion={motionReduced ? "reduced" : "full"}
    >
      <div className="relative aspect-square overflow-hidden bg-[var(--color-surface-soft)]">
        {productHref ? (
          <Link
            href={productHref}
            aria-label={`View details for ${product.name}`}
            className="block h-full w-full"
          >
            <Image
              src={toAssetUrl(product.imageUrl)}
              alt={product.name}
              width={400}
              height={400}
              sizes="(max-width: 768px) 45vw, (max-width: 1280px) 30vw, 22vw"
              className={mediaClassName}
            />
          </Link>
        ) : (
          <Image
            src={toAssetUrl(product.imageUrl)}
            alt={product.name}
            width={400}
            height={400}
            sizes="(max-width: 768px) 45vw, (max-width: 1280px) 30vw, 22vw"
            className={mediaClassName}
          />
        )}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
        <div className="absolute left-3 top-3 z-10 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-slate-700">
          {product.brand}
        </div>
        {product.id ? <QuickProductActions productId={productId} initiallyWishlisted={product.wishlisted} /> : null}
      </div>

      <div className="space-y-3 p-5">
        <h3 className="text-lg font-semibold text-slate-900">{product.name}</h3>
        <div className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
          <span>{product.storage || "N/A"}</span>
          <span>-</span>
          <span>{product.ram || "N/A"}</span>
          <span>-</span>
          <span>{product.size || "N/A"}</span>
        </div>
        <p className="text-2xl font-bold text-[var(--color-primary-strong)]">
          {formatPriceVnd(product.price)}
        </p>
        <p
          className={`text-sm font-medium ${
            product.available ? "text-[var(--color-success)]" : "text-red-600"
          }`}
        >
          {product.availabilityLabel || "Unavailable"}
        </p>

        {productHref ? (
          <Link
            href={productHref}
            className="ui-btn ui-btn-primary group/details inline-flex w-full items-center justify-center gap-2 px-4 py-2.5 text-sm"
          >
            <span className="relative inline-flex h-4 w-4 items-center justify-center">
              <GriddyIcon
                name="eye-closed"
                className="absolute inset-0 transition-[opacity,transform] duration-150 group-hover/details:opacity-0 group-hover/details:scale-95 group-focus-visible/details:opacity-0 group-focus-visible/details:scale-95"
              />
              <GriddyIcon
                name="eye"
                className="absolute inset-0 opacity-0 transition-[opacity,transform] duration-150 group-hover/details:opacity-100 group-hover/details:scale-100 group-focus-visible/details:opacity-100 group-focus-visible/details:scale-100"
              />
            </span>
            View Details
          </Link>
        ) : null}
      </div>
    </article>
  );
}
