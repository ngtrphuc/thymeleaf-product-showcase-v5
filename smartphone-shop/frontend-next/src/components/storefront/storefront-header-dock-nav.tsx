"use client";

import { type ComponentType, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  ClipboardList,
  Heart,
  LayoutDashboard,
  LogIn,
  LogOut,
  ShoppingCart,
  SlidersHorizontal,
  UserRound,
} from "lucide-react";
import { Dock, DockIcon, DockItem, DockLabel } from "@/components/ui/dock";
import { ApiError, authLogout, getBackendOrigin, type AuthMeResponse } from "@/lib/api";

type NavItem = {
  key: string;
  label: string;
  href?: string;
  icon: ComponentType<{ className?: string }>;
};

const authenticatedDockItems: NavItem[] = [
  { key: "cart", label: "Cart", href: "/cart", icon: ShoppingCart },
  { key: "orders", label: "Orders", href: "/orders", icon: ClipboardList },
  { key: "wishlist", label: "Wishlist", href: "/wishlist", icon: Heart },
  { key: "compare", label: "Compare", href: "/compare", icon: SlidersHorizontal },
  { key: "profile", label: "Profile", href: "/profile", icon: UserRound },
];

const guestDockItems: NavItem[] = [
  { key: "compare", label: "Compare", href: "/compare", icon: SlidersHorizontal },
  { key: "login", label: "Login", icon: LogIn },
];

const adminDockItems: NavItem[] = [
  { key: "admin-panel", label: "Admin Panel", href: "/admin", icon: LayoutDashboard },
  { key: "logout", label: "Logout", icon: LogOut },
];

export function StorefrontHeaderDockNav() {
  const router = useRouter();
  const pathname = usePathname();
  const [authState, setAuthState] = useState<AuthMeResponse>({
    authenticated: false,
    email: null,
    role: null,
    fullName: null,
  });
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const response = await fetch(`${getBackendOrigin()}/api/v1/auth/me`, {
          credentials: "include",
        });

        if (!response.ok) {
          if (alive) {
            setAuthState({
              authenticated: false,
              email: null,
              role: null,
              fullName: null,
            });
          }
          return;
        }

        const body = (await response.json()) as AuthMeResponse;
        if (alive) {
          setAuthState({
            authenticated: Boolean(body?.authenticated),
            email: body?.email ?? null,
            role: body?.role ?? null,
            fullName: body?.fullName ?? null,
          });
        }
      } catch {
        if (alive) {
          setAuthState({
            authenticated: false,
            email: null,
            role: null,
            fullName: null,
          });
        }
      }
    }

    void resolveAuth();
    return () => {
      alive = false;
    };
  }, []);

  const dockItems = useMemo(() => {
    if (authState.role === "ROLE_ADMIN") {
      return adminDockItems;
    }
    return authState.authenticated ? authenticatedDockItems : guestDockItems;
  }, [authState]);

  function isActive(item: NavItem): boolean {
    if (!item.href) {
      return false;
    }
    return pathname === item.href || pathname.startsWith(`${item.href}/`);
  }

  async function onNavigate(item: NavItem) {
    if (loggingOut) {
      return;
    }

    if (item.key === "login") {
      const next = encodeURIComponent(pathname || "/products");
      router.push(`/login?next=${next}`);
      return;
    }

    if (item.key === "logout") {
      setLoggingOut(true);
      try {
        await authLogout();
        router.push("/login");
        router.refresh();
      } catch (error) {
        if (error instanceof ApiError) {
          console.error(error.message);
        } else {
          console.error("Logout failed.");
        }
        setLoggingOut(false);
      }
      return;
    }

    if (item.href) {
      router.push(item.href);
    }
  }

  return (
    <Dock className="h-auto">
      {dockItems.map((item) => {
        const Icon = item.icon;
        return (
          <DockItem
            key={item.key}
            ariaLabel={item.label}
            active={isActive(item)}
            onClick={() => void onNavigate(item)}
          >
            <DockIcon>
              <Icon className="h-4 w-4" />
            </DockIcon>
            <DockLabel>{item.label}</DockLabel>
          </DockItem>
        );
      })}
    </Dock>
  );
}
