export type Theme = "dark" | "light";
export type ThemeScope = "storefront" | "admin";

export const THEME_KEY_PREFIX = "smartphone-shop-theme";
export const CURRENT_ACCOUNT_EMAIL_STORAGE_KEY = `${THEME_KEY_PREFIX}:current-account-email`;
export const THEME_CHANGED_EVENT = "smartphone-shop:theme-changed";
export const AUTH_CHANGED_EVENT = "smartphone-shop:auth-changed";

export function accountGlobalThemeKey(email: string): string {
  return `${THEME_KEY_PREFIX}:user:${email.trim().toLowerCase()}`;
}

export function accountThemeKey(scope: ThemeScope, email: string): string {
  return `${THEME_KEY_PREFIX}:${scope}:user:${email.trim().toLowerCase()}`;
}

export function parseTheme(value: string | null): Theme | null {
  return value === "light" || value === "dark" ? value : null;
}

export function resolveScopeFromPath(pathname: string): ThemeScope {
  const path = (pathname || "").toLowerCase();
  return path === "/admin" || path.startsWith("/admin/") ? "admin" : "storefront";
}

export function normalizeAccountEmail(email: string | null | undefined): string | null {
  if (!email) {
    return null;
  }
  const normalized = email.trim().toLowerCase();
  return normalized.length > 0 ? normalized : null;
}

export function getCurrentAccountEmailFromStorage(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    return normalizeAccountEmail(window.localStorage.getItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY));
  } catch {
    return null;
  }
}

export function setCurrentAccountEmail(email: string | null): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    const normalized = normalizeAccountEmail(email);
    if (normalized) {
      window.localStorage.setItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY, normalized);
    } else {
      window.localStorage.removeItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY);
    }
  } catch {
    // Ignore storage access issues.
  }
}

export function resolveTheme(scope: ThemeScope, accountEmail: string | null): Theme {
  if (typeof window === "undefined") {
    return "light";
  }

  try {
    const normalized = normalizeAccountEmail(accountEmail);
    if (!normalized) {
      return "light";
    }
    const globalTheme = parseTheme(window.localStorage.getItem(accountGlobalThemeKey(normalized)));
    if (globalTheme) {
      return globalTheme;
    }

    const scopedTheme = parseTheme(window.localStorage.getItem(accountThemeKey(scope, normalized)));
    if (scopedTheme) {
      return scopedTheme;
    }

    const fallbackScope: ThemeScope = scope === "admin" ? "storefront" : "admin";
    return parseTheme(window.localStorage.getItem(accountThemeKey(fallbackScope, normalized))) ?? "light";
  } catch {
    return "light";
  }
}

export function persistTheme(theme: Theme, scope: ThemeScope, accountEmail: string | null): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    const normalized = normalizeAccountEmail(accountEmail);
    if (!normalized) {
      return;
    }
    window.localStorage.setItem(accountGlobalThemeKey(normalized), theme);
    window.localStorage.setItem(accountThemeKey(scope, normalized), theme);
  } catch {
    // Ignore storage access issues.
  }
}

export function applyTheme(theme: Theme, animate: boolean): void {
  if (typeof document === "undefined") {
    return;
  }

  const root = document.documentElement;
  const currentTheme = root.getAttribute("data-theme");
  if (currentTheme === theme && !animate) {
    return;
  }

  if (animate && currentTheme !== theme) {
    root.setAttribute("data-theme-transitioning", "");
    window.requestAnimationFrame(() => {
      root.setAttribute("data-theme", theme);
      window.setTimeout(() => {
        root.removeAttribute("data-theme-transitioning");
      }, 560);
    });
    return;
  }

  root.setAttribute("data-theme", theme);
}

export function broadcastThemeChange(theme: Theme, scope: ThemeScope): void {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent(THEME_CHANGED_EVENT, { detail: { theme, scope } }));
}

export function broadcastAuthChange(accountEmail: string | null): void {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT, { detail: { accountEmail } }));
}
