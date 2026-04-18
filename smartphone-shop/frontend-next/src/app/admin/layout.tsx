import Link from "next/link";
import { AdminSessionActions } from "@/components/admin/admin-session-actions";

export default function AdminLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <Link href="/admin" className="text-lg font-bold text-slate-900">
            Admin Console
          </Link>
          <AdminSessionActions />
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
