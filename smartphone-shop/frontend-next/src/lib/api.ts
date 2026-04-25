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
  os: string | null;
  chipset: string | null;
  speed: string | null;
  resolution: string | null;
  battery: string | null;
  charging: string | null;
  description: string | null;
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

export type AuthMeResponse = {
  authenticated: boolean;
  email: string | null;
  role: string | null;
  fullName: string | null;
};

export type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  email: string;
  role: string;
  fullName: string;
};

export type OperationStatusResponse = {
  success: boolean;
  message: string;
};

export type CartItemResponse = {
  id: number;
  name: string;
  price: number;
  quantity: number;
  imageUrl: string | null;
  availableStock: number;
  lineTotal: number;
  lowStock: boolean;
  availabilityLabel: string;
};

export type CartResponse = {
  items: CartItemResponse[];
  totalAmount: number;
  itemCount: number;
  authenticated: boolean;
};

export type PaymentMethodResponse = {
  id: number;
  type: string;
  displayName: string;
  maskedDetail: string | null;
  isDefault: boolean;
  active: boolean;
  createdAt: string;
};

export type ProfileResponse = {
  id: number;
  email: string;
  fullName: string;
  phoneNumber: string | null;
  defaultAddress: string | null;
  deliveredOrderCount: number;
  pendingOrderCount: number;
  cartItemCount: number;
  paymentMethods: PaymentMethodResponse[];
};

export type OrderItemResponse = {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
};

export type OrderResponse = {
  id: number;
  orderCode: string;
  status: string;
  statusSummary: string;
  customerName: string;
  phoneNumber: string;
  shippingAddress: string;
  totalAmount: number;
  paymentMethod: string;
  paymentPlan: string;
  installmentMonths: number | null;
  installmentMonthlyAmount: number | null;
  createdAt: string;
  itemCount: number;
  cancelable: boolean;
  items: OrderItemResponse[];
};

export type WishlistItemResponse = {
  productId: number;
  name: string;
  price: number;
  imageUrl: string | null;
  stock: number;
  addedAt: string;
};

export type WishlistResponse = {
  items: WishlistItemResponse[];
  count: number;
};

export type CompareResponse = {
  products: ProductSummary[];
  ids: number[];
  maxCompare: number;
};

export type ChatMessageResponse = {
  id: number;
  userEmail: string;
  content: string;
  senderRole: string;
  createdAt: string;
};

export type AdminDashboardResponse = {
  totalProducts: number;
  totalItemsSold: number;
  totalOrders: number;
  totalRevenue: number;
  currentPage: number;
  totalPages: number;
  recentOrders: OrderResponse[];
};

export type AdminProductPageResponse = {
  products: AdminProduct[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  brands: string[];
};

export type AdminProduct = {
  id: number | null;
  name: string;
  price: number | null;
  imageUrl: string | null;
  stock: number | null;
  os: string | null;
  chipset: string | null;
  speed: string | null;
  ram: string | null;
  storage: string | null;
  size: string | null;
  resolution: string | null;
  battery: string | null;
  charging: string | null;
  description: string | null;
};

export type AdminOrderPageResponse = {
  orders: OrderResponse[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
};

export type AdminConversationsResponse = {
  emails: string[];
  unreadCounts: Record<string, number>;
};

const DEFAULT_BACKEND_ORIGIN = "http://localhost:8080";
const BACKEND_ASSET_PREFIX = "/asset-proxy";
const RETRYABLE_STATUS_CODES = new Set([408, 425, 429, 500, 502, 503, 504]);
const RETRY_SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
const DEFAULT_RETRY_COUNT = Number.parseInt(process.env.NEXT_PUBLIC_API_RETRY_COUNT ?? "1", 10);
const DEFAULT_RETRY_BASE_DELAY_MS = Number.parseInt(process.env.NEXT_PUBLIC_API_RETRY_BASE_DELAY_MS ?? "250", 10);
const DEFAULT_AUTH_ME_CACHE_TTL_MS = Number.parseInt(process.env.NEXT_PUBLIC_AUTH_ME_CACHE_TTL_MS ?? "30000", 10);
const COMPARE_UPDATED_EVENT = "storefront:compare-updated";

let authMeCache: AuthMeResponse | null = null;
let authMeCacheAt = 0;
let authMeInFlight: Promise<AuthMeResponse> | null = null;

type CompareUpdatedListener = (compare: CompareResponse) => void;

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
  const backendOrigin = getBackendOrigin();

  if (/^https?:\/\//i.test(path)) {
    try {
      const url = new URL(path);
      if (url.origin === backendOrigin) {
        return `${BACKEND_ASSET_PREFIX}${url.pathname}${url.search}`;
      }
    } catch {
      return path;
    }
    return path;
  }

  if (path.startsWith("/")) {
    return `${BACKEND_ASSET_PREFIX}${path}`;
  }

  return `${BACKEND_ASSET_PREFIX}/${path.replace(/^\/+/, "")}`;
}

function notifyCompareUpdated(compare: CompareResponse): void {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent<CompareResponse>(COMPARE_UPDATED_EVENT, { detail: compare }));
}

export function subscribeCompareUpdated(listener: CompareUpdatedListener): () => void {
  if (typeof window === "undefined") {
    return () => {};
  }

  const eventListener: EventListener = (event) => {
    const customEvent = event as CustomEvent<CompareResponse>;
    if (!customEvent.detail) {
      return;
    }
    listener(customEvent.detail);
  };

  window.addEventListener(COMPARE_UPDATED_EVENT, eventListener);
  return () => window.removeEventListener(COMPARE_UPDATED_EVENT, eventListener);
}

type RequestOptions = RequestInit & {
  includeCredentials?: boolean;
  skipAuthRedirect?: boolean;
  next?: {
    revalidate?: number;
  };
};

function normalizeMethod(method: string | undefined): string {
  return (method ?? "GET").toUpperCase();
}

function resolveRetryCount(): number {
  if (!Number.isFinite(DEFAULT_RETRY_COUNT)) {
    return 1;
  }
  return Math.max(0, DEFAULT_RETRY_COUNT);
}

function resolveRetryDelay(attempt: number): number {
  const safeBaseDelay = Number.isFinite(DEFAULT_RETRY_BASE_DELAY_MS) ? DEFAULT_RETRY_BASE_DELAY_MS : 250;
  const baseDelay = Math.max(0, safeBaseDelay);
  return baseDelay * (attempt + 1);
}

function shouldRetryRequest(status: number, method: string, attempt: number, maxRetries: number): boolean {
  return attempt < maxRetries && RETRY_SAFE_METHODS.has(method) && RETRYABLE_STATUS_CODES.has(status);
}

function shouldRetryNetworkError(method: string, attempt: number, maxRetries: number): boolean {
  return attempt < maxRetries && RETRY_SAFE_METHODS.has(method);
}

function shouldRedirectToLogin(status: number, options?: RequestOptions): boolean {
  return (
    (status === 401 || status === 403) &&
    options?.skipAuthRedirect !== true &&
    typeof window !== "undefined" &&
    typeof window.location !== "undefined"
  );
}

function redirectToLoginPage(options?: { reauth?: boolean }): void {
  if (typeof window === "undefined") {
    return;
  }
  const loginUrl = new URL("/login", window.location.origin);
  const nextPath = `${window.location.pathname}${window.location.search}`;
  loginUrl.searchParams.set("next", nextPath);
  if (options?.reauth) {
    loginUrl.searchParams.set("reauth", "1");
  }
  window.location.assign(loginUrl.toString());
}

async function parseApiError(response: Response): Promise<ApiError> {
  let message = `API request failed (${response.status}).`;
  try {
    const errorBody = (await response.json()) as { message?: string };
    if (errorBody?.message) {
      message = errorBody.message;
    }
  } catch {
    // ignore parse failure
  }
  return new ApiError(message, response.status);
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function requestJson<T>(path: string, init?: RequestOptions): Promise<T> {
  const headers = new Headers(init?.headers);
  if (!headers.has("Content-Type") && init?.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const method = normalizeMethod(init?.method);
  const maxRetries = resolveRetryCount();

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch(`${getBackendOrigin()}${path}`, {
        ...init,
        headers,
        method,
        credentials: init?.includeCredentials === false ? "omit" : "include",
      });

      if (response.ok) {
        if (response.status === 204) {
          return undefined as T;
        }
        return (await response.json()) as T;
      }

      if (shouldRedirectToLogin(response.status, init)) {
        invalidateAuthMeCache();
        redirectToLoginPage({ reauth: response.status === 403 });
      }

      if (shouldRetryRequest(response.status, method, attempt, maxRetries)) {
        await delay(resolveRetryDelay(attempt));
        continue;
      }

      throw await parseApiError(response);
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      if (shouldRetryNetworkError(method, attempt, maxRetries)) {
        await delay(resolveRetryDelay(attempt));
        continue;
      }
      throw new ApiError("Network request failed. Please try again.", 0);
    }
  }

  throw new ApiError("Network request failed. Please try again.", 0);
}

export async function fetchCatalogPage(searchParams: URLSearchParams): Promise<CatalogPageResponse> {
  const query = searchParams.toString();
  const suffix = query.length > 0 ? `?${query}` : "";
  return requestJson<CatalogPageResponse>(`/api/v1/products${suffix}`, {
    next: { revalidate: 20 },
    includeCredentials: false,
  });
}

export async function fetchProductDetail(id: string): Promise<ProductDetailResponse> {
  return requestJson<ProductDetailResponse>(`/api/v1/products/${id}`, {
    next: { revalidate: 20 },
    includeCredentials: false,
  });
}

export async function fetchAuthMe(): Promise<AuthMeResponse> {
  const response = await requestJson<AuthMeResponse>("/api/v1/auth/me");
  authMeCache = response;
  authMeCacheAt = Date.now();
  return response;
}

export function invalidateAuthMeCache(): void {
  authMeCache = null;
  authMeCacheAt = 0;
  authMeInFlight = null;
}

type FetchAuthMeCachedOptions = {
  ttlMs?: number;
  force?: boolean;
};

export async function fetchAuthMeCached(options?: FetchAuthMeCachedOptions): Promise<AuthMeResponse> {
  const configuredTtlMs = Number.isFinite(DEFAULT_AUTH_ME_CACHE_TTL_MS) ? DEFAULT_AUTH_ME_CACHE_TTL_MS : 30000;
  const ttlCandidate = options?.ttlMs;
  const ttlMs = Math.max(0, Number.isFinite(ttlCandidate) ? (ttlCandidate ?? configuredTtlMs) : configuredTtlMs);
  const now = Date.now();
  if (!options?.force && authMeCache && now - authMeCacheAt < ttlMs) {
    return authMeCache;
  }
  if (!options?.force && authMeInFlight) {
    return authMeInFlight;
  }

  authMeInFlight = fetchAuthMe()
    .catch((error) => {
      invalidateAuthMeCache();
      throw error;
    })
    .finally(() => {
      authMeInFlight = null;
    });
  return authMeInFlight;
}

export async function authLogin(email: string, password: string): Promise<AuthTokenResponse> {
  const response = await requestJson<AuthTokenResponse>("/api/v1/auth/login", {
    method: "POST",
    skipAuthRedirect: true,
    body: JSON.stringify({ email, password }),
  });
  invalidateAuthMeCache();
  return response;
}

export async function authRegister(email: string, fullName: string, password: string): Promise<OperationStatusResponse> {
  const response = await requestJson<OperationStatusResponse>("/api/v1/auth/register", {
    method: "POST",
    skipAuthRedirect: true,
    body: JSON.stringify({ email, fullName, password }),
  });
  invalidateAuthMeCache();
  return response;
}

export async function authLogout(): Promise<OperationStatusResponse> {
  const response = await requestJson<OperationStatusResponse>("/api/v1/auth/logout", {
    method: "POST",
  });
  invalidateAuthMeCache();
  return response;
}

export async function fetchCart(): Promise<CartResponse> {
  return requestJson<CartResponse>("/api/v1/cart");
}

export async function addCartItem(productId: number, quantity = 1): Promise<CartResponse> {
  return requestJson<CartResponse>("/api/v1/cart/items", {
    method: "POST",
    body: JSON.stringify({ productId, quantity }),
  });
}

export async function increaseCartItem(productId: number): Promise<CartResponse> {
  return requestJson<CartResponse>(`/api/v1/cart/items/${productId}/increase`, { method: "POST" });
}

export async function decreaseCartItem(productId: number): Promise<CartResponse> {
  return requestJson<CartResponse>(`/api/v1/cart/items/${productId}/decrease`, { method: "POST" });
}

export async function removeCartItem(productId: number): Promise<CartResponse> {
  return requestJson<CartResponse>(`/api/v1/cart/items/${productId}`, { method: "DELETE" });
}

export async function clearCart(): Promise<CartResponse> {
  return requestJson<CartResponse>("/api/v1/cart", { method: "DELETE" });
}

export type PlaceOrderPayload = {
  customerName: string;
  phoneNumber: string;
  shippingAddress: string;
  paymentMethod: string;
  paymentDetail?: string | null;
  paymentPlan: string;
  installmentMonths?: number | null;
};

export async function placeOrder(payload: PlaceOrderPayload, idempotencyKey: string): Promise<OrderResponse> {
  return requestJson<OrderResponse>("/api/v1/orders", {
    method: "POST",
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
    body: JSON.stringify(payload),
  });
}

export async function fetchOrders(): Promise<OrderResponse[]> {
  return requestJson<OrderResponse[]>("/api/v1/orders");
}

export async function cancelOrder(orderId: number): Promise<OperationStatusResponse> {
  return requestJson<OperationStatusResponse>(`/api/v1/orders/${orderId}/cancel`, {
    method: "POST",
  });
}

export async function fetchProfile(): Promise<ProfileResponse> {
  return requestJson<ProfileResponse>("/api/v1/profile");
}

export async function updateProfile(payload: {
  fullName: string;
  phoneNumber?: string | null;
  defaultAddress?: string | null;
}): Promise<ProfileResponse> {
  return requestJson<ProfileResponse>("/api/v1/profile", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function fetchPaymentMethods(): Promise<PaymentMethodResponse[]> {
  return requestJson<PaymentMethodResponse[]>("/api/v1/payment-methods");
}

export async function addPaymentMethod(payload: {
  type: string;
  bankDetail?: string | null;
  setAsDefault?: boolean;
}): Promise<PaymentMethodResponse[]> {
  return requestJson<PaymentMethodResponse[]>("/api/v1/payment-methods", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function setDefaultPaymentMethod(id: number): Promise<PaymentMethodResponse[]> {
  return requestJson<PaymentMethodResponse[]>(`/api/v1/payment-methods/${id}/default`, {
    method: "POST",
  });
}

export async function removePaymentMethod(id: number): Promise<PaymentMethodResponse[]> {
  return requestJson<PaymentMethodResponse[]>(`/api/v1/payment-methods/${id}`, {
    method: "DELETE",
  });
}

export async function fetchWishlist(): Promise<WishlistResponse> {
  return requestJson<WishlistResponse>("/api/v1/wishlist");
}

export async function addWishlistItem(productId: number): Promise<WishlistResponse> {
  return requestJson<WishlistResponse>("/api/v1/wishlist/items", {
    method: "POST",
    body: JSON.stringify({ productId }),
  });
}

export async function removeWishlistItem(productId: number): Promise<WishlistResponse> {
  return requestJson<WishlistResponse>(`/api/v1/wishlist/items/${productId}`, {
    method: "DELETE",
  });
}

export async function fetchCompare(): Promise<CompareResponse> {
  return requestJson<CompareResponse>("/api/v1/compare");
}

export async function addCompareItem(productId: number): Promise<CompareResponse> {
  const response = await requestJson<CompareResponse>("/api/v1/compare/items", {
    method: "POST",
    body: JSON.stringify({ productId }),
  });
  notifyCompareUpdated(response);
  return response;
}

export async function removeCompareItem(productId: number): Promise<CompareResponse> {
  const response = await requestJson<CompareResponse>(`/api/v1/compare/items/${productId}`, {
    method: "DELETE",
  });
  notifyCompareUpdated(response);
  return response;
}

export async function clearCompare(): Promise<CompareResponse> {
  const response = await requestJson<CompareResponse>("/api/v1/compare", { method: "DELETE" });
  notifyCompareUpdated(response);
  return response;
}

export async function replaceCompareItems(productIds: number[]): Promise<CompareResponse> {
  const response = await requestJson<CompareResponse>("/api/v1/compare", {
    method: "PUT",
    body: JSON.stringify({ productIds }),
  });
  notifyCompareUpdated(response);
  return response;
}

export async function fetchChatHistory(): Promise<ChatMessageResponse[]> {
  return requestJson<ChatMessageResponse[]>("/api/v1/chat/history");
}

export async function sendChatMessage(content: string): Promise<ChatMessageResponse> {
  return requestJson<ChatMessageResponse>("/api/v1/chat/messages", {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export async function markChatRead(): Promise<OperationStatusResponse> {
  return requestJson<OperationStatusResponse>("/api/v1/chat/read", {
    method: "POST",
  });
}

export async function fetchChatUnreadCount(): Promise<number> {
  return requestJson<number>("/api/v1/chat/unread-count");
}

export function openChatEventStream(): EventSource {
  return new EventSource(`${getBackendOrigin()}/api/v1/chat/stream`, { withCredentials: true });
}

export async function fetchAdminDashboard(page = 0): Promise<AdminDashboardResponse> {
  return requestJson<AdminDashboardResponse>(`/api/v1/admin/dashboard?page=${page}`);
}

export async function fetchAdminProducts(params: URLSearchParams): Promise<AdminProductPageResponse> {
  const query = params.toString();
  const suffix = query ? `?${query}` : "";
  return requestJson<AdminProductPageResponse>(`/api/v1/admin/products${suffix}`);
}

export async function createAdminProduct(payload: AdminProduct): Promise<AdminProduct> {
  return requestJson<AdminProduct>("/api/v1/admin/products", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateAdminProduct(id: number, payload: AdminProduct): Promise<AdminProduct> {
  return requestJson<AdminProduct>(`/api/v1/admin/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminProduct(id: number): Promise<OperationStatusResponse> {
  return requestJson<OperationStatusResponse>(`/api/v1/admin/products/${id}`, {
    method: "DELETE",
  });
}

export async function fetchAdminOrders(page = 0, pageSize = 10): Promise<AdminOrderPageResponse> {
  return requestJson<AdminOrderPageResponse>(`/api/v1/admin/orders?page=${page}&pageSize=${pageSize}`);
}

export async function updateAdminOrderStatus(id: number, status: string): Promise<OperationStatusResponse> {
  return requestJson<OperationStatusResponse>(`/api/v1/admin/orders/${id}/status`, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}

export async function fetchAdminConversations(): Promise<AdminConversationsResponse> {
  return requestJson<AdminConversationsResponse>("/api/v1/admin/chat/conversations");
}

export async function fetchAdminChatHistory(email: string): Promise<ChatMessageResponse[]> {
  const escaped = encodeURIComponent(email);
  return requestJson<ChatMessageResponse[]>(`/api/v1/admin/chat/history?email=${escaped}`);
}

export async function sendAdminChatMessage(userEmail: string, content: string): Promise<ChatMessageResponse> {
  return requestJson<ChatMessageResponse>("/api/v1/admin/chat/messages", {
    method: "POST",
    body: JSON.stringify({ userEmail, content }),
  });
}

export async function markAdminConversationRead(userEmail: string): Promise<OperationStatusResponse> {
  return requestJson<OperationStatusResponse>("/api/v1/admin/chat/read", {
    method: "POST",
    body: JSON.stringify({ userEmail }),
  });
}

export async function fetchAdminUnreadCount(): Promise<number> {
  return requestJson<number>("/api/v1/admin/chat/unread-count");
}

export function openAdminChatEventStream(): EventSource {
  return new EventSource(`${getBackendOrigin()}/api/v1/admin/chat/stream`, { withCredentials: true });
}
