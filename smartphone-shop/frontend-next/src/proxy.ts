import { NextRequest, NextResponse } from "next/server";

const CUSTOMER_PROTECTED = ["/cart", "/checkout", "/profile", "/orders", "/wishlist", "/chat"];
const ADMIN_PROTECTED = ["/admin"];
const AUTH_ROUTES = ["/login", "/register"];

function startsWithAny(pathname: string, rules: string[]): boolean {
  return rules.some((rule) => pathname === rule || pathname.startsWith(`${rule}/`));
}

function decodeJwtPayload(token: string): { role?: string; exp?: number } | null {
  // This decode is only for client-side route UX (redirect hints).
  // Real authorization is enforced by backend role checks.
  const segments = token.split(".");
  if (segments.length < 2) {
    return null;
  }

  try {
    const encoded = segments[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "=");
    return JSON.parse(atob(padded)) as { role?: string; exp?: number };
  } catch {
    return null;
  }
}

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("jwt")?.value;
  const payload = token ? decodeJwtPayload(token) : null;
  const nowInSeconds = Math.floor(Date.now() / 1000);
  const tokenExpired = payload?.exp !== undefined && payload.exp <= nowInSeconds;
  const role = tokenExpired ? null : payload?.role ?? null;

  const requiresCustomerAuth = startsWithAny(pathname, CUSTOMER_PROTECTED);
  const requiresAdminAuth = startsWithAny(pathname, ADMIN_PROTECTED);

  if ((requiresCustomerAuth || requiresAdminAuth) && (!token || tokenExpired)) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", pathname);
    if (tokenExpired) {
      const response = NextResponse.redirect(loginUrl);
      response.cookies.delete("jwt");
      return response;
    }
    return NextResponse.redirect(loginUrl);
  }

  if (requiresAdminAuth && token && !tokenExpired) {
    if (!isAdminRole(role)) {
      return NextResponse.redirect(new URL("/products", request.url));
    }
  }

  if (token && !tokenExpired && startsWithAny(pathname, AUTH_ROUTES)) {
    const isReauthFlow =
      (pathname === "/login" || pathname === "/register") && request.nextUrl.searchParams.get("reauth") === "1";
    if (isReauthFlow) {
      return NextResponse.next();
    }
    const destination = isAdminRole(role) ? "/admin" : "/products";
    return NextResponse.redirect(new URL(destination, request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
