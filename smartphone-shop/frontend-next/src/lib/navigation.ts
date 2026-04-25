const DEFAULT_AUTH_REDIRECT = "/products";
const LOCAL_ORIGIN = "http://smartphone-shop.local";

export function isSafeInternalPath(value: string | null | undefined): value is string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.includes("\\")) {
    return false;
  }

  try {
    const parsed = new URL(value, LOCAL_ORIGIN);
    return parsed.origin === LOCAL_ORIGIN && parsed.pathname.startsWith("/");
  } catch {
    return false;
  }
}

export function resolveSafeInternalPath(
  value: string | null | undefined,
  fallback = DEFAULT_AUTH_REDIRECT,
): string {
  return isSafeInternalPath(value) ? value : fallback;
}
