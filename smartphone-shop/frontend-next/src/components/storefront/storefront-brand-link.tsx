"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, fetchAuthMeCached } from "@/lib/api";
import { AUTH_CHANGED_EVENT } from "@/lib/theme";
import { GriddyIcon } from "@/components/ui/griddy-icon";

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

export function StorefrontBrandLink() {
  const [admin, setAdmin] = useState(false);

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const auth = await fetchAuthMeCached();
        if (alive) {
          setAdmin(auth.authenticated && isAdminRole(auth.role));
        }
      } catch (error) {
        if (alive && !(error instanceof ApiError)) {
          setAdmin(false);
        }
      }
    }

    void resolveAuth();
    window.addEventListener(AUTH_CHANGED_EVENT, resolveAuth);
    return () => {
      alive = false;
      window.removeEventListener(AUTH_CHANGED_EVENT, resolveAuth);
    };
  }, []);

  return (
    <Link href={admin ? "/admin" : "/products"} className="brand-glow-link text-lg font-bold text-slate-900">
      <GriddyIcon name={admin ? "dashboard" : "spark"} className="h-[1.15rem] w-[1.15rem]" />
      {admin ? "Admin Panel" : "Smartphone Shop"}
    </Link>
  );
}
