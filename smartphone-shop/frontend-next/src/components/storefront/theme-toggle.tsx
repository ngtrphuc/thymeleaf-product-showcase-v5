"use client";

import { useCallback, useEffect, useState } from "react";
import {
  applyTheme,
  AUTH_CHANGED_EVENT,
  broadcastThemeChange,
  getCurrentAccountEmailFromStorage,
  persistTheme,
  resolveTheme,
  THEME_CHANGED_EVENT,
  type Theme,
  type ThemeScope,
} from "@/lib/theme";

type ThemeToggleProps = {
  scope?: ThemeScope;
};

export function ThemeToggle({ scope = "storefront" }: ThemeToggleProps) {
  const [theme, setTheme] = useState<Theme>(() => resolveTheme(scope, getCurrentAccountEmailFromStorage()));
  const [tilting, setTilting] = useState(false);

  useEffect(() => {
    function syncThemeState() {
      setTheme(resolveTheme(scope, getCurrentAccountEmailFromStorage()));
    }

    syncThemeState();
    window.addEventListener(AUTH_CHANGED_EVENT, syncThemeState);
    window.addEventListener(THEME_CHANGED_EVENT, syncThemeState);
    window.addEventListener("storage", syncThemeState);
    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, syncThemeState);
      window.removeEventListener(THEME_CHANGED_EVENT, syncThemeState);
      window.removeEventListener("storage", syncThemeState);
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
    persistTheme(nextTheme, scope, getCurrentAccountEmailFromStorage());
    broadcastThemeChange(nextTheme, scope);
  }, [scope, theme]);

  return (
    <div className="flex items-center gap-3">
      <span className="text-xs font-medium" style={{ color: "var(--color-text-muted)" }}>
        {theme === "dark" ? "Dark" : "Light"}
      </span>
      <button
        type="button"
        role="switch"
        aria-checked={theme === "dark"}
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
