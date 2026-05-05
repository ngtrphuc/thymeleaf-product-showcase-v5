"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchAuthMeCached } from "@/lib/api";

type Theme = "dark" | "light";
type ThemeScope = "storefront" | "admin";

const THEME_KEY_PREFIX = "smartphone-shop-theme";
const CURRENT_ACCOUNT_EMAIL_STORAGE_KEY = `${THEME_KEY_PREFIX}:current-account-email`;

function guestThemeKey(scope: ThemeScope): string {
  return `${THEME_KEY_PREFIX}:${scope}:guest`;
}

function accountThemeKey(scope: ThemeScope, email: string): string {
  return `${THEME_KEY_PREFIX}:${scope}:user:${email.trim().toLowerCase()}`;
}

function parseTheme(value: string | null): Theme | null {
  if (value === "light" || value === "dark") {
    return value;
  }
  return null;
}

function getStoredTheme(scope: ThemeScope, accountEmail: string | null): Theme {
  if (typeof window === "undefined") {
    return "light";
  }

  try {
    if (accountEmail) {
      const accountTheme = parseTheme(localStorage.getItem(accountThemeKey(scope, accountEmail)));
      if (accountTheme) {
        return accountTheme;
      }
      return "light";
    }

    const guestTheme = parseTheme(localStorage.getItem(guestThemeKey(scope)));
    if (guestTheme) {
      return guestTheme;
    }
  } catch {
    // Ignore storage access issues and fallback to light.
  }

  return "light";
}

function getCurrentAccountEmailFromStorage(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const value = window.localStorage.getItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY);
    if (!value) {
      return null;
    }
    const normalized = value.trim().toLowerCase();
    return normalized.length > 0 ? normalized : null;
  } catch {
    return null;
  }
}

function applyTheme(theme: Theme, animate: boolean): void {
  const root = document.documentElement;

  if (animate) {
    root.setAttribute("data-theme-transitioning", "");
    requestAnimationFrame(() => {
      root.setAttribute("data-theme", theme);
      window.setTimeout(() => {
        root.removeAttribute("data-theme-transitioning");
      }, 560);
    });
  } else {
    root.setAttribute("data-theme", theme);
  }

}

function persistTheme(theme: Theme, scope: ThemeScope, accountEmail: string | null): void {
  try {
    if (accountEmail) {
      localStorage.setItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY, accountEmail.trim().toLowerCase());
      localStorage.setItem(accountThemeKey(scope, accountEmail), theme);
      return;
    }

    localStorage.removeItem(CURRENT_ACCOUNT_EMAIL_STORAGE_KEY);
    localStorage.setItem(guestThemeKey(scope), theme);
  } catch {
    // Ignore storage access issues.
  }
}

type ThemeToggleProps = {
  scope?: ThemeScope;
};

export function ThemeToggle({ scope = "storefront" }: ThemeToggleProps) {
  const [accountEmail, setAccountEmail] = useState<string | null>(() => getCurrentAccountEmailFromStorage());
  const [theme, setTheme] = useState<Theme>(() => getStoredTheme(scope, getCurrentAccountEmailFromStorage()));
  const [tilting, setTilting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    void fetchAuthMeCached()
      .then((auth) => {
        if (cancelled) {
          return;
        }
        const email = auth.authenticated && auth.email ? auth.email : null;
        setAccountEmail(email);
        const preferredTheme = getStoredTheme(scope, email);
        setTheme(preferredTheme);
        applyTheme(preferredTheme, false);
        persistTheme(preferredTheme, scope, email);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        setAccountEmail(null);
        const guestTheme = getStoredTheme(scope, null);
        setTheme(guestTheme);
        applyTheme(guestTheme, false);
      });

    return () => {
      cancelled = true;
    };
  }, [scope]);

  const toggle = useCallback(() => {
    const nextTheme: Theme = theme === "dark" ? "light" : "dark";

    setTilting(true);
    window.setTimeout(() => {
      setTilting(false);
    }, 420);

    setTheme(nextTheme);
    applyTheme(nextTheme, true);
    persistTheme(nextTheme, scope, accountEmail);
  }, [accountEmail, scope, theme]);

  return (
    <div className="flex items-center gap-3">
      <span className="text-xs font-medium" style={{ color: "var(--color-text-muted)" }}>
        {theme === "dark" ? "Dark" : "Light"}
      </span>
      <button
        type="button"
        role="switch"
        aria-checked={theme === "light"}
        aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
        className="theme-switch"
        data-mode={theme}
        onClick={toggle}
      >
        <svg
          className="theme-switch-icon-sun"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
        </svg>

        <svg
          className="theme-switch-icon-moon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>

        <div className={`theme-switch-knob${tilting ? " is-tilting" : ""}`} />
      </button>
    </div>
  );
}
