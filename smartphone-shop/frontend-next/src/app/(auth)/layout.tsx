import Link from "next/link";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function AuthLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-4 py-8">
      <header className="mb-4 text-center">
        <Link href="/products" className="inline-flex items-center gap-2 text-lg font-bold text-slate-900">
          <GriddyIcon name="spark" />
          Smartphone Shop
        </Link>
      </header>
      {children}
    </div>
  );
}
