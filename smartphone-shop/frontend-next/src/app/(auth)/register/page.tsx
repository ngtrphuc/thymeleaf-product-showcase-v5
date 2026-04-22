"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, authRegister } from "@/lib/api";
import { PasswordField } from "@/components/auth/password-field";
import { GriddyIcon } from "@/components/ui/griddy-icon";

function getAuthRedirectParams(): { nextPath: string; reauthFlow: boolean } {
  if (typeof window === "undefined") {
    return { nextPath: "/products", reauthFlow: false };
  }
  const search = new URLSearchParams(window.location.search);
  const next = search.get("next");
  return {
    nextPath: next && next.startsWith("/") ? next : "/products",
    reauthFlow: search.get("reauth") === "1",
  };
}

export default function RegisterPage() {
  const router = useRouter();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [{ nextPath, reauthFlow }] = useState(getAuthRedirectParams);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const result = await authRegister(email, fullName, password);
      setMessage(result.message || "Registration successful. Redirecting to login...");
      setTimeout(() => {
        const loginHref = `/login?next=${encodeURIComponent(nextPath)}${reauthFlow ? "&reauth=1" : ""}`;
        router.push(loginHref);
      }, 900);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Registration failed.");
      }
      setLoading(false);
    }
  }

  return (
    <div className="glass-panel rounded-3xl p-6">
      <h1 className="text-2xl font-bold text-slate-900">Register</h1>
      <p className="mt-2 text-sm text-slate-600">Create your customer account.</p>

      {error ? <p className="mt-3 text-sm text-red-700">{error}</p> : null}
      {message ? <p className="mt-3 text-sm text-emerald-700">{message}</p> : null}

      <form className="mt-4 space-y-3" onSubmit={onSubmit}>
        <label className="block space-y-1">
          <span className="text-sm text-slate-700">Full Name</span>
          <input
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            className="ui-input w-full px-3 py-2 text-sm"
            required
          />
        </label>

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
          autoComplete="new-password"
          required
        />

        <PasswordField
          id="confirm-password"
          name="confirmPassword"
          label="Confirm Password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
          required
        />

        <button
          type="submit"
          disabled={loading}
          className="ui-btn ui-btn-primary inline-flex w-full items-center justify-center gap-2 px-4 py-2.5 text-sm"
        >
          <GriddyIcon name="user" />
          {loading ? "Creating account..." : "Create Account"}
        </button>
      </form>

      <p className="mt-4 text-sm text-slate-600">
        Already registered?{" "}
        <Link
          href={`/login?next=${encodeURIComponent(nextPath)}${reauthFlow ? "&reauth=1" : ""}`}
          className="font-semibold text-[var(--color-primary-strong)]"
        >
          Sign In
        </Link>
      </p>
    </div>
  );
}
