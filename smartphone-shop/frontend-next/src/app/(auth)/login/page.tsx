/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, authLogin } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nextPath, setNextPath] = useState("/products");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const search = new URLSearchParams(window.location.search);
    const next = search.get("next");
    if (next && next.startsWith("/")) {
      setNextPath(next);
    }
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const auth = await authLogin(email, password);
      const destination = auth.role === "ROLE_ADMIN" ? "/admin" : nextPath;
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
            className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
            required
          />
        </label>

        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Password</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm"
            required
          />
        </label>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-[var(--color-primary)] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
        >
          {loading ? "Signing in..." : "Sign In"}
        </button>
      </form>

      <p className="mt-4 text-sm text-slate-600">
        New user?{" "}
        <Link href="/register" className="font-semibold text-[var(--color-primary-strong)]">
          Create account
        </Link>
      </p>
    </div>
  );
}

