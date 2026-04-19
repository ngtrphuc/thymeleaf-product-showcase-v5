"use client";

import { type ComponentType, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  ClipboardList,
  Heart,
  LogIn,
  MessageSquare,
  ShoppingCart,
  SlidersHorizontal,
  UserRound,
} from "lucide-react";
import { Dock, DockIcon, DockItem, DockLabel } from "@/components/ui/dock";
import { getBackendOrigin, type AuthMeResponse } from "@/lib/api";

type NavItem = {
  key: string;
  label: string;
  href?: string;
  icon: ComponentType<{ className?: string }>;
};

const authenticatedDockItems: NavItem[] = [
  { key: "cart", label: "Cart", href: "/cart", icon: ShoppingCart },
  { key: "orders", label: "Orders", href: "/orders", icon: ClipboardList },
  { key: "chat", label: "Chat", href: "/chat", icon: MessageSquare },
  { key: "wishlist", label: "Wishlist", href: "/wishlist", icon: Heart },
  { key: "compare", label: "Compare", href: "/compare", icon: SlidersHorizontal },
  { key: "profile", label: "Profile", href: "/profile", icon: UserRound },
];

const guestDockItems: NavItem[] = [
  { key: "compare", label: "Compare", href: "/compare", icon: SlidersHorizontal },
  { key: "login", label: "Login", icon: LogIn },
];

export function StorefrontHeaderDockNav() {
  const router = useRouter();
  const pathname = usePathname();
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const response = await fetch(`${getBackendOrigin()}/api/v1/auth/me`, {
          credentials: "include",
        });

        if (!response.ok) {
          if (alive) {
            setAuthenticated(false);
          }
          return;
        }

        const body = (await response.json()) as AuthMeResponse;
        if (alive) {
          setAuthenticated(Boolean(body?.authenticated));
        }
      } catch {
        if (alive) {
          setAuthenticated(false);
        }
      }
    }

    void resolveAuth();
    return () => {
      alive = false;
    };
  }, []);

  const dockItems = useMemo(
    () => (authenticated ? authenticatedDockItems : guestDockItems),
    [authenticated],
  );

  function isActive(item: NavItem): boolean {
    if (!item.href) {
      return false;
    }
    return pathname === item.href || pathname.startsWith(`${item.href}/`);
  }

  function onNavigate(item: NavItem) {
    if (item.key === "login") {
      const next = encodeURIComponent(pathname || "/products");
      router.push(`/login?next=${next}`);
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
            onClick={() => onNavigate(item)}
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
