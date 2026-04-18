import Link from "next/link";
import { ExpandingNav } from "@/components/ui/expanding-nav";
import { GriddyIcon } from "@/components/ui/griddy-icon";

const storefrontLinks = [
  { href: "/products", label: "Products", icon: "package" },
  { href: "/cart", label: "Cart", icon: "cart" },
  { href: "/orders", label: "Orders", icon: "orders" },
  { href: "/chat", label: "Chat Shop", icon: "chat" },
  { href: "/wishlist", label: "Wishlist", icon: "wishlist" },
  { href: "/compare", label: "Compare", icon: "clipboard" },
  { href: "/profile", label: "Profile", icon: "profile" },
];

export default function StorefrontLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <Link href="/products" className="inline-flex items-center gap-2 text-lg font-bold text-slate-900">
            <GriddyIcon name="spark" className="h-[1.15rem] w-[1.15rem]" />
            Smartphone Shop
          </Link>
          <ExpandingNav items={storefrontLinks} ariaLabel="Storefront navigation" />
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
