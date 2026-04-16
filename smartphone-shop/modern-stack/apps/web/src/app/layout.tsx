import type { Metadata } from 'next';
import Link from 'next/link';

import './globals.css';

export const metadata: Metadata = {
  title: 'Smartphone Shop Modern',
  description: 'Overnight migration build: Next.js + NestJS + Postgres + Redis',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <header className="topbar">
          <div className="brand">Smartphone Shop 2026</div>
          <nav>
            <Link href="/">Storefront</Link>
            <Link href="/cart">Cart</Link>
            <Link href="/admin">Admin</Link>
          </nav>
        </header>
        <main className="shell">{children}</main>
      </body>
    </html>
  );
}
