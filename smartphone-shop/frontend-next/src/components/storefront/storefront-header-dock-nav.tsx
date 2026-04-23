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
import { ApiError, authLogout, fetchAuthMeCached, type AuthMeResponse } from "@/lib/api";

type NavItem = {
  key: string;
  label: string;
  href?: string;
  icon: ComponentType<{ className?: string; strokeWidth?: number }>;
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
  { key: "login", label: "Sign In", icon: LogIn },
];

const adminDockItems: NavItem[] = [
  { key: "compare", label: "Compare", href: "/compare", icon: SlidersHorizontal },
  { key: "admin-panel", label: "Admin Panel", href: "/admin", icon: LayoutDashboard },
  { key: "logout", label: "Sign Out", icon: LogOut },
];

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

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
        const body = await fetchAuthMeCached();
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
    if (isAdminRole(authState.role)) {
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
        router.push("/login?reauth=1");
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
      const requiresFreshAuthCheck =
        item.key === "cart" || item.key === "orders" || item.key === "wishlist" || item.key === "profile";
      if (requiresFreshAuthCheck) {
        try {
          const latestAuth = await fetchAuthMeCached({ force: true });
          if (!latestAuth.authenticated) {
            const next = encodeURIComponent(item.href);
            router.push(`/login?next=${next}&reauth=1`);
            return;
          }
        } catch {
          const next = encodeURIComponent(item.href);
          router.push(`/login?next=${next}&reauth=1`);
          return;
        }
      }
      router.push(item.href);
    }
  }

  return (
    <Dock className="h-auto">
      {dockItems.map((item) => {
        const Icon = item.icon;
        const itemActive = isActive(item);
        return (
          <DockItem
            key={item.key}
            ariaLabel={item.label}
            active={itemActive}
            activeLabel={item.label}
            onClick={() => void onNavigate(item)}
            className={item.key === "login" ? "dock-item-login" : item.key === "logout" ? "dock-item-logout" : ""}
          >
            <DockIcon>
              <Icon className={itemActive ? "h-[1.06rem] w-[1.06rem]" : "h-4 w-4"} strokeWidth={itemActive ? 2.45 : 2} />
            </DockIcon>
            {!itemActive ? <DockLabel>{item.label}</DockLabel> : null}
          </DockItem>
        );
      })}
    </Dock>
  );
}
