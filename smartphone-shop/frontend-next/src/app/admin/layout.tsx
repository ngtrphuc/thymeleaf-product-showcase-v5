import Link from "next/link";
import { AdminHeaderNav } from "@/components/admin/admin-header-nav";
import { AdminSessionActions } from "@/components/admin/admin-session-actions";
import { ThemeToggle } from "@/components/storefront/theme-toggle";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function AdminLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <Link href="/products" className="brand-glow-link text-lg font-bold text-slate-900">
              <GriddyIcon name="spark" className="h-[1.15rem] w-[1.15rem]" />
              Smartphone Shop
            </Link>
            <AdminHeaderNav />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <ThemeToggle scope="admin" />
            <AdminSessionActions />
          </div>
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}

