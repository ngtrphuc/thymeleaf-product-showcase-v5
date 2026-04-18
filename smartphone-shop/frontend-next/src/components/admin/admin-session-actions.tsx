"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError, authLogout } from "@/lib/api";

export function AdminSessionActions() {
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function logout() {
    setLoggingOut(true);
    setError(null);
    try {
      await authLogout();
      router.push("/login");
      router.refresh();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Logout failed.");
      }
      setLoggingOut(false);
    }
  }

  return (
    <div className="flex items-center gap-2">
      <Link
        href="/admin"
        className="rounded-lg border border-transparent px-3 py-1.5 text-sm font-medium text-slate-700 hover:border-[var(--color-border)] hover:bg-white"
      >
        Admin
      </Link>
      <button
        type="button"
        onClick={() => void logout()}
        disabled={loggingOut}
        className="rounded-lg border border-transparent px-3 py-1.5 text-sm font-medium text-slate-700 hover:border-[var(--color-border)] hover:bg-white disabled:opacity-60"
      >
        {loggingOut ? "Logging out..." : "Logout"}
      </button>
      {error ? <span className="text-xs text-red-700">{error}</span> : null}
    </div>
  );
}
