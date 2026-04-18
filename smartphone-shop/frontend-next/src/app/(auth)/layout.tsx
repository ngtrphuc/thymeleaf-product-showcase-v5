import Link from "next/link";

export default function AuthLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-4 py-8">
      <header className="mb-4 text-center">
        <Link href="/products" className="text-lg font-bold text-slate-900">
          Smartphone Shop
        </Link>
      </header>
      {children}
    </div>
  );
}
