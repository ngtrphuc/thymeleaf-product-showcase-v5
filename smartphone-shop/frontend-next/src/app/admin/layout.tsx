import Link from "next/link";
import { AdminSessionActions } from "@/components/admin/admin-session-actions";
import { ExpandingNav } from "@/components/ui/expanding-nav";

const adminLinks = [
  { href: "/admin", label: "Dashboard", icon: "dashboard" },
  { href: "/admin/products", label: "Products", icon: "box" },
  { href: "/admin/orders", label: "Orders", icon: "orders" },
  { href: "/admin/chat", label: "Chat", icon: "chat" },
];

export default function AdminLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <Link href="/admin" className="text-lg font-bold text-slate-900">
              Admin Console
            </Link>
            <ExpandingNav items={adminLinks} ariaLabel="Admin navigation" />
          </div>
          <AdminSessionActions />
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
