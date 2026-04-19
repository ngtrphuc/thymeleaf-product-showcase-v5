import Image from "next/image";
import Link from "next/link";
import type { ProductSummary } from "@/lib/api";
import { toAssetUrl } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { QuickProductActions } from "@/components/storefront/quick-product-actions";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type ProductCardProps = {
  product: ProductSummary;
};

export function ProductCard({ product }: ProductCardProps) {
  const productId = product.id ?? 0;

  return (
    <article className="glass-panel group overflow-hidden rounded-3xl">
      <div className="relative aspect-square overflow-hidden bg-[var(--color-surface-soft)]">
        <Image
          src={toAssetUrl(product.imageUrl)}
          alt={product.name}
          width={640}
          height={640}
          className="h-full w-full object-contain p-2 transition duration-300 group-hover:scale-[1.03]"
          unoptimized
        />
        <div className="absolute left-3 top-3 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-slate-700">
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

        <Link
          href={`/products/${productId}`}
          className="ui-btn ui-btn-primary inline-flex w-full items-center justify-center px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="eye" className="mr-1" />
          View Details
        </Link>
      </div>
    </article>
  );
}
