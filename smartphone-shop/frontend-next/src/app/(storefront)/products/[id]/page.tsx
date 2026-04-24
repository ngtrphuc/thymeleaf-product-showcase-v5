import Image from "next/image";
import Link from "next/link";
import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import { ApiError, fetchProductDetail, getBackendOrigin, toAssetUrl, type AuthMeResponse, type ProductSummary } from "@/lib/api";
import { formatPriceVnd } from "@/lib/format";
import { ProductActions } from "@/components/storefront/product-actions";

type ProductDetailPageProps = {
  params: Promise<{ id: string }>;
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

function unauthenticatedAuthState(): AuthMeResponse {
  return {
    authenticated: false,
    email: null,
    role: null,
    fullName: null,
  };
}

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

async function resolveAuthState(cookieHeader: string): Promise<AuthMeResponse> {
  if (!cookieHeader.includes("jwt=")) {
    return unauthenticatedAuthState();
  }
  try {
    const response = await fetch(`${getBackendOrigin()}/api/v1/auth/me`, {
      headers: {
        Cookie: cookieHeader,
      },
      cache: "no-store",
    });
    if (!response.ok) {
      return unauthenticatedAuthState();
    }
    const payload = (await response.json()) as AuthMeResponse;
    if (!payload.authenticated) {
      return unauthenticatedAuthState();
    }
    return payload;
  } catch {
    return unauthenticatedAuthState();
  }
}

function SpecItem({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-semibold text-slate-900">{value || "N/A"}</dd>
    </div>
  );
}

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { id } = await params;
  const cookieStore = await cookies();
  const authState = await resolveAuthState(cookieStore.toString());
  const isAdmin = isAdminRole(authState.role);
  const isAuthenticated = authState.authenticated;

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
            sizes="(max-width: 768px) 100vw, 50vw"
            className="h-full w-full object-contain p-2"
          />
        </div>

        <div className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{product.brand}</p>
          <h1 className="text-3xl font-bold text-slate-900">{product.name}</h1>
          <AvailabilityBadge product={product} />
          <p className="text-3xl font-bold text-[var(--color-primary-strong)]">{formatPriceVnd(product.price)}</p>

          <div className="rounded-2xl border border-[var(--color-border)] bg-white p-4">
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <SpecItem label="Storage" value={product.storage} />
              <SpecItem label="RAM" value={product.ram} />
              <SpecItem label="Screen" value={product.size} />
              <SpecItem label="Resolution" value={product.resolution} />
              <SpecItem label="Operating System" value={product.os} />
              <SpecItem label="Chipset" value={product.chipset} />
              <SpecItem label="CPU Speed" value={product.speed} />
              <SpecItem label="Battery" value={product.battery} />
              <SpecItem label="Charging" value={product.charging} />
              <SpecItem label="Stock" value={product.stock} />
            </dl>
          </div>

          {product.description ? (
            <div className="rounded-2xl border border-[var(--color-border)] bg-white p-4">
              <h2 className="text-sm font-semibold text-slate-900">Product Description</h2>
              <p className="mt-2 text-sm leading-6 text-slate-700">{product.description}</p>
            </div>
          ) : null}

          <p className="text-sm text-slate-600">
            Estimated installment from <strong>{formatPriceVnd(product.monthlyInstallmentAmount)}</strong> / month.
          </p>
          {product.id ? (
            <ProductActions
              productId={product.id}
              isAdmin={isAdmin}
              isAuthenticated={isAuthenticated && !isAdmin}
              editHref="/admin/products"
              backHref="/products"
              maxQuantity={product.stock}
            />
          ) : null}
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
            {detail.recommendedProducts.map((recommended) =>
              recommended.id ? (
                <Link
                  key={recommended.id}
                  href={`/products/${recommended.id}`}
                  className="glass-panel rounded-2xl p-3 hover:-translate-y-px"
                >
                  <Image
                    src={toAssetUrl(recommended.imageUrl)}
                    alt={recommended.name}
                    width={480}
                    height={480}
                    sizes="(max-width: 768px) 50vw, 25vw"
                    className="aspect-square w-full rounded-xl bg-[var(--color-surface-soft)] object-contain p-2"
                  />
                  <p className="mt-2 text-sm font-semibold text-slate-900">{recommended.name}</p>
                  <p className="mt-1 text-sm font-bold text-[var(--color-primary-strong)]">
                    {formatPriceVnd(recommended.price)}
                  </p>
                </Link>
              ) : (
                <article
                  key={`${recommended.name}-${recommended.brand}`}
                  className="glass-panel rounded-2xl p-3 opacity-70"
                >
                  <Image
                    src={toAssetUrl(recommended.imageUrl)}
                    alt={recommended.name}
                    width={480}
                    height={480}
                    sizes="(max-width: 768px) 50vw, 25vw"
                    className="aspect-square w-full rounded-xl bg-[var(--color-surface-soft)] object-contain p-2"
                  />
                  <p className="mt-2 text-sm font-semibold text-slate-900">{recommended.name}</p>
                  <p className="mt-1 text-sm font-bold text-[var(--color-primary-strong)]">
                    {formatPriceVnd(recommended.price)}
                  </p>
                </article>
              ),
            )}
          </div>
        )}
      </section>
    </div>
  );
}
