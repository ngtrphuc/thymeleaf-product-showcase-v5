export type ProductSummary = {
  id: number | null;
  name: string;
  brand: string;
  price: number | null;
  imageUrl: string | null;
  stock: number | null;
  available: boolean;
  lowStock: boolean;
  availabilityLabel: string;
  monthlyInstallmentAmount: number;
  storage: string;
  ram: string;
  size: string;
  wishlisted: boolean;
};

export type CatalogPageResponse = {
  products: ProductSummary[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  brands: string[];
  activeFilterCount: number;
  hasActiveFilters: boolean;
};

export type ProductDetailResponse = {
  product: ProductSummary;
  recommendedProducts: ProductSummary[];
  wishlisted: boolean;
};

type RevalidateOption = {
  next?: {
    revalidate?: number;
  };
};

const DEFAULT_BACKEND_ORIGIN = "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function getBackendOrigin(): string {
  const configured =
    process.env.API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    DEFAULT_BACKEND_ORIGIN;
  return configured.replace(/\/+$/, "");
}

export function toAssetUrl(path: string | null | undefined): string {
  if (!path) {
    return "https://placehold.co/640x640/e2e8f0/475569?text=No+Image";
  }
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  if (path.startsWith("/")) {
    return `${getBackendOrigin()}${path}`;
  }
  return `${getBackendOrigin()}/${path}`;
}

async function requestJson<T>(
  path: string,
  init?: RequestInit & RevalidateOption,
): Promise<T> {
  const response = await fetch(`${getBackendOrigin()}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let message = `API request failed (${response.status}).`;
    try {
      const errorBody = (await response.json()) as { message?: string };
      if (errorBody?.message) {
        message = errorBody.message;
      }
    } catch {
      // Keep fallback message if body is not JSON.
    }
    throw new ApiError(message, response.status);
  }

  return (await response.json()) as T;
}

export async function fetchCatalogPage(
  searchParams: URLSearchParams,
): Promise<CatalogPageResponse> {
  const query = searchParams.toString();
  const suffix = query.length > 0 ? `?${query}` : "";
  return requestJson<CatalogPageResponse>(`/api/v1/products${suffix}`, {
    next: { revalidate: 20 },
  });
}

export async function fetchProductDetail(
  id: string,
): Promise<ProductDetailResponse> {
  return requestJson<ProductDetailResponse>(`/api/v1/products/${id}`, {
    next: { revalidate: 20 },
  });
}
