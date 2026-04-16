import type { ApiEnvelope, ProductSummary } from '@smartphone-shop/shared';

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:4000/api/v1';

export async function fetchProducts(keyword?: string): Promise<ProductSummary[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
  const response = await fetch(`${API_BASE}/products${query}`, {
    cache: 'no-store',
  });
  if (!response.ok) {
    return [];
  }

  const body = (await response.json()) as ApiEnvelope<{ items: ProductSummary[]; total: number }>;
  return body.data?.items ?? [];
}

export async function fetchHealth(): Promise<string> {
  const response = await fetch(`${API_BASE}/health`, { cache: 'no-store' });
  if (!response.ok) {
    return 'down';
  }
  const body = (await response.json()) as ApiEnvelope<{ status: string }>;
  return body.data?.status ?? 'down';
}
