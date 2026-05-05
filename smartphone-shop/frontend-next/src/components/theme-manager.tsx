"use client";

import { useCallback, useEffect } from "react";
import { usePathname } from "next/navigation";
import { fetchAuthMeCached } from "@/lib/api";
import {
  applyTheme,
  AUTH_CHANGED_EVENT,
  CURRENT_ACCOUNT_EMAIL_STORAGE_KEY,
  getCurrentAccountEmailFromStorage,
  normalizeAccountEmail,
  resolveScopeFromPath,
  resolveTheme,
  setCurrentAccountEmail,
  THEME_CHANGED_EVENT,
  THEME_KEY_PREFIX,
} from "@/lib/theme";

export function ThemeManager() {
  const pathname = usePathname();

  const reapplyTheme = useCallback((animate: boolean) => {
    const scope = resolveScopeFromPath(window.location.pathname);
    const email = getCurrentAccountEmailFromStorage();
    applyTheme(resolveTheme(scope, email), animate);
  }, []);

  useEffect(() => {
    const scope = resolveScopeFromPath(pathname || "/");
    const email = getCurrentAccountEmailFromStorage();
    applyTheme(resolveTheme(scope, email), false);
  }, [pathname]);

  useEffect(() => {
    let alive = true;

    async function syncAuthFromServer() {
      try {
        const auth = await fetchAuthMeCached({ force: true });
        if (!alive) {
          return;
        }
        const email = auth.authenticated ? normalizeAccountEmail(auth.email) : null;
        setCurrentAccountEmail(email);
        reapplyTheme(true);
      } catch {
        if (!alive) {
          return;
        }
        setCurrentAccountEmail(null);
        reapplyTheme(true);
      }
    }

    function onAuthChanged() {
      reapplyTheme(true);
    }

    function onThemeChanged() {
      reapplyTheme(false);
    }

    function onStorage(event: StorageEvent) {
      if (
        event.key === null ||
        event.key === CURRENT_ACCOUNT_EMAIL_STORAGE_KEY ||
        (typeof event.key === "string" && event.key.startsWith(`${THEME_KEY_PREFIX}:`))
      ) {
        reapplyTheme(true);
      }
    }

    window.addEventListener(AUTH_CHANGED_EVENT, onAuthChanged);
    window.addEventListener(THEME_CHANGED_EVENT, onThemeChanged);
    window.addEventListener("storage", onStorage);
    void syncAuthFromServer();

    return () => {
      alive = false;
      window.removeEventListener(AUTH_CHANGED_EVENT, onAuthChanged);
      window.removeEventListener(THEME_CHANGED_EVENT, onThemeChanged);
      window.removeEventListener("storage", onStorage);
    };
  }, [reapplyTheme]);

  return null;
}
