import Link from "next/link";

const storefrontLinks = [
  { href: "/products", label: "Products" },
  { href: "/cart", label: "Cart" },
  { href: "/checkout", label: "Checkout" },
  { href: "/orders", label: "Orders" },
  { href: "/wishlist", label: "Wishlist" },
  { href: "/compare", label: "Compare" },
  { href: "/profile", label: "Profile" },
];

export default function StorefrontLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <Link href="/products" className="text-lg font-bold text-slate-900">
            Smartphone Shop
          </Link>
          <nav className="flex flex-wrap gap-2 text-sm">
            {storefrontLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-lg border border-transparent px-3 py-1.5 font-medium text-slate-700 hover:border-[var(--color-border)] hover:bg-white"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
