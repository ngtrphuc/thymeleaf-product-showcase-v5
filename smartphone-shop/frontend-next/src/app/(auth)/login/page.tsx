/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, authLogin } from "@/lib/api";
import { resolveSafeInternalPath } from "@/lib/navigation";
import { PasswordField } from "@/components/auth/password-field";
import { AuthMotionIcon } from "@/components/ui/auth-motion-icon";

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

function resolveLoginDestination(role: string | null | undefined, nextPath: string): string {
  if (isAdminRole(role)) {
    return nextPath;
  }
  return nextPath.startsWith("/admin") ? "/products" : nextPath;
}

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nextPath, setNextPath] = useState("/products");
  const [reauthFlow, setReauthFlow] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const search = new URLSearchParams(window.location.search);
    setNextPath(resolveSafeInternalPath(search.get("next")));
    setReauthFlow(search.get("reauth") === "1");
  }, []);

  const registerHref = `/register?next=${encodeURIComponent(nextPath)}${reauthFlow ? "&reauth=1" : ""}`;

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const auth = await authLogin(email, password);
      const destination = resolveLoginDestination(auth.role, nextPath);
      router.push(destination);
      router.refresh();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Login failed.");
      }
      setLoading(false);
    }
  }

  return (
    <div className="glass-panel rounded-3xl p-6">
      <h1 className="text-2xl font-bold text-slate-900">Login</h1>
      <p className="mt-2 text-sm text-slate-600">Sign in to access cart, checkout, and profile.</p>

      {error ? <p className="mt-3 text-sm text-red-700">{error}</p> : null}

      <form className="mt-4 space-y-3" onSubmit={onSubmit}>
        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Email</span>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="ui-input w-full px-3 py-2 text-sm"
            required
          />
        </label>

        <PasswordField
          id="password"
          name="password"
          label="Password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
          required
        />

        <button
          type="submit"
          disabled={loading}
          className="ui-btn ui-btn-login inline-flex w-full items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <AuthMotionIcon variant="login" className="h-4 w-4" />
          {loading ? "Signing in..." : "Sign In"}
        </button>
      </form>

      <p className="mt-4 text-sm text-slate-600">
        New user?{" "}
        <Link href={registerHref} className="font-semibold text-[var(--color-primary-strong)]">
          Create account
        </Link>
      </p>
    </div>
  );
}

