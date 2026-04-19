"use client";

import { useEffect, useState } from "react";
import { ExpandingNav } from "@/components/ui/expanding-nav";
import { ApiError, fetchAdminUnreadCount } from "@/lib/api";

const adminLinks = [
  { href: "/admin", label: "Dashboard", icon: "dashboard" },
  { href: "/admin/products", label: "Products", icon: "box" },
  { href: "/admin/orders", label: "Orders", icon: "orders" },
  { href: "/admin/chat", label: "Chat", icon: "chat" },
] as const;

export function AdminHeaderNav() {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadUnreadCount() {
      try {
        const count = await fetchAdminUnreadCount();
        if (!cancelled) {
          setUnreadCount(count);
        }
      } catch (error) {
        if (!cancelled && !(error instanceof ApiError)) {
          setUnreadCount(0);
        }
      }
    }

    void loadUnreadCount();
    const timer = window.setInterval(() => {
      void loadUnreadCount();
    }, 8000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  return (
    <ExpandingNav
      ariaLabel="Admin navigation"
      items={adminLinks.map((item) =>
        item.href === "/admin/chat"
          ? {
              ...item,
              badge: unreadCount > 0 ? unreadCount : undefined,
            }
          : item,
      )}
    />
  );
}
