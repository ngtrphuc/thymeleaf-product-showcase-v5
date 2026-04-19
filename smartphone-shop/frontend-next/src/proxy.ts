import { NextRequest, NextResponse } from "next/server";

const CUSTOMER_PROTECTED = ["/cart", "/checkout", "/profile", "/orders", "/wishlist", "/chat"];
const ADMIN_PROTECTED = ["/admin"];
const AUTH_ROUTES = ["/login", "/register"];

function startsWithAny(pathname: string, rules: string[]): boolean {
  return rules.some((rule) => pathname === rule || pathname.startsWith(`${rule}/`));
}

function decodeJwtRole(token: string): string | null {
  // This decode is only for client-side route UX (redirect hints).
  // Real authorization is enforced by backend role checks.
  const segments = token.split(".");
  if (segments.length < 2) {
    return null;
  }

  try {
    const encoded = segments[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "=");
    const payload = JSON.parse(atob(padded)) as { role?: string };
    return payload.role ?? null;
  } catch {
    return null;
  }
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("jwt")?.value;
  const role = token ? decodeJwtRole(token) : null;

  const requiresCustomerAuth = startsWithAny(pathname, CUSTOMER_PROTECTED);
  const requiresAdminAuth = startsWithAny(pathname, ADMIN_PROTECTED);

  if ((requiresCustomerAuth || requiresAdminAuth) && !token) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", pathname);
    return NextResponse.redirect(loginUrl);
  }

  if (requiresAdminAuth && token) {
    if (role !== "ROLE_ADMIN") {
      return NextResponse.redirect(new URL("/products", request.url));
    }
  }

  if (token && startsWithAny(pathname, AUTH_ROUTES)) {
    const destination = role === "ROLE_ADMIN" ? "/admin" : "/products";
    return NextResponse.redirect(new URL(destination, request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
