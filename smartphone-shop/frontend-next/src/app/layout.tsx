import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
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
  title: "Smartphone Shop",
  description: "Next.js frontend for Smartphone Shop",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full bg-[var(--color-page)] text-[var(--color-text)]">
        <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(1100px_circle_at_10%_0%,rgba(31,114,255,0.18),transparent_45%),radial-gradient(900px_circle_at_90%_10%,rgba(11,179,146,0.18),transparent_40%),linear-gradient(180deg,#f4f8ff_0%,#eef6f5_100%)]" />
        {children}
      </body>
    </html>
  );
}
