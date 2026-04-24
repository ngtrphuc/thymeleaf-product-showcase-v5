"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { fetchAuthMeCached, type AuthMeResponse } from "@/lib/api";

const SOCIAL_LOGIN_LINKS = [
  {
    name: "Facebook",
    href: "https://www.facebook.com/login/",
    iconPath: "/sns/facebook.svg",
  },
  {
    name: "Instagram",
    href: "https://www.instagram.com/accounts/login/",
    iconPath: "/sns/instagram.svg",
  },
  {
    name: "X",
    href: "https://x.com/i/flow/login",
    iconPath: "/sns/x.svg",
  },
  {
    name: "LINE",
    href: "https://access.line.me/oauth2/v2.1/login",
    iconPath: "/sns/line.svg",
  },
  {
    name: "TikTok",
    href: "https://www.tiktok.com/login",
    iconPath: "/sns/tiktok.svg",
  },
  {
    name: "YouTube",
    href: "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/",
    iconPath: "/sns/youtube.svg",
  },
] as const;

type QuickLink = {
  href: string;
  label: string;
};

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

export function StorefrontFooter() {
  const currentYear = new Date().getFullYear();
  const [authState, setAuthState] = useState<AuthMeResponse>({
    authenticated: false,
    email: null,
    role: null,
    fullName: null,
  });

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const body = await fetchAuthMeCached();
        if (!alive) {
          return;
        }
        setAuthState({
          authenticated: Boolean(body?.authenticated),
          email: body?.email ?? null,
          role: body?.role ?? null,
          fullName: body?.fullName ?? null,
        });
      } catch {
        if (!alive) {
          return;
        }
        setAuthState({
          authenticated: false,
          email: null,
          role: null,
          fullName: null,
        });
      }
    }

    void resolveAuth();
    return () => {
      alive = false;
    };
  }, []);

  const quickLinks = useMemo<QuickLink[]>(() => {
    const links: QuickLink[] = [
      { href: "/products", label: "Browse Products" },
      { href: "/compare", label: "Compare Phones" },
    ];

    if (authState.authenticated && !isAdminRole(authState.role)) {
      links.splice(1, 0, { href: "/cart", label: "Cart" }, { href: "/orders", label: "Track Orders" });
    }

    if (authState.authenticated && isAdminRole(authState.role)) {
      links.push({ href: "/admin", label: "Admin Panel" });
    }

    return links;
  }, [authState]);

  return (
    <footer className="glass-panel mt-10 rounded-3xl px-6 py-8 sm:px-8">
      <div className="grid gap-8 lg:grid-cols-[1.35fr_1fr_1fr]">
        <section>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--color-text-faint)]">
            Smartphone Shop
          </p>
          <h2 className="mt-2 text-2xl font-bold text-slate-900">Buy Fast, Compare Smart, Checkout Safe.</h2>
          <p className="mt-3 max-w-md text-sm text-[var(--color-text-muted)]">
            Your trusted phone and accessories store with transparent pricing, fast checkout, and reliable support
            every day.
          </p>
        </section>

        <section>
          <h3 className="text-sm font-semibold text-slate-900">Quick Links</h3>
          <ul className="mt-3 space-y-2 text-sm text-[var(--color-text-muted)]">
            {quickLinks.map((item) => (
              <li key={item.href}>
                <Link href={item.href} className="hover:text-white">
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h3 className="text-sm font-semibold text-slate-900">Customer Care</h3>
          <ul className="mt-3 space-y-2 text-sm text-[var(--color-text-muted)]">
            <li>Support: support@smartphoneshop.local</li>
            <li>Hotline: +81 XXXX XXXX</li>
            <li>Open daily: 08:00 - 22:00</li>
            <li>Secure payment and order protection</li>
          </ul>
        </section>
      </div>

      <div className="mt-8 border-t border-[var(--color-border)] pt-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-2">
            {SOCIAL_LOGIN_LINKS.map((social) => (
              <a
                key={social.name}
                href={social.href}
                target="_blank"
                rel="noreferrer noopener"
                aria-label={`Open ${social.name} login page`}
                title={`${social.name} login`}
                className="ui-btn group relative inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-0 text-xs text-[var(--color-text-muted)] transition-[transform,background-color,color,border-color,box-shadow] duration-200 hover:-translate-y-px hover:border-white/12 hover:bg-white hover:text-black hover:shadow-[0_8px_18px_rgba(255,255,255,0.16)]"
              >
                <Image
                  src={social.iconPath}
                  alt={`${social.name} logo`}
                  width={14}
                  height={14}
                  className="h-[14px] w-[14px] object-contain transition-[filter,transform] duration-200 group-hover:invert"
                />
                <span className="pointer-events-none absolute -top-9 left-1/2 z-20 -translate-x-1/2 whitespace-nowrap rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] font-medium text-[var(--color-text)] opacity-0 shadow-[0_4px_18px_rgba(0,0,0,0.35)] transition-opacity duration-150 group-hover:opacity-100 group-focus-visible:opacity-100">
                  {social.name}
                </span>
              </a>
            ))}
          </div>
          <p className="text-xs text-[var(--color-text-faint)]">(c) {currentYear} Smartphone Shop. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
