"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError, authLogout } from "@/lib/api";
import { GriddyIcon } from "@/components/ui/griddy-icon";

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
      <button
        type="button"
        onClick={() => void logout()}
        disabled={loggingOut}
        className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium"
      >
        <GriddyIcon name="logout" />
        {loggingOut ? "Logging out..." : "Logout"}
      </button>
      {error ? <span className="text-xs text-red-700">{error}</span> : null}
    </div>
  );
}
