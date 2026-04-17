import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Smartphone Shop Frontend",
  description: "Next.js storefront for Smartphone Shop API",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-[var(--color-page)] text-[var(--color-text)]">
        <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(1100px_circle_at_10%_0%,rgba(31,114,255,0.18),transparent_45%),radial-gradient(900px_circle_at_90%_10%,rgba(11,179,146,0.18),transparent_40%),linear-gradient(180deg,#f4f8ff_0%,#eef6f5_100%)]" />
        <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
          <header className="glass-panel mb-6 flex items-center justify-between rounded-2xl px-5 py-4">
            <Link href="/products" className="text-base font-bold text-slate-900">
              Smartphone Shop
            </Link>
            <a
              href="http://localhost:8080/swagger-ui/index.html"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm font-medium text-[var(--color-primary-strong)] hover:text-[var(--color-primary)]"
            >
              API Docs
            </a>
          </header>
          {children}
        </div>
      </body>
    </html>
  );
}
