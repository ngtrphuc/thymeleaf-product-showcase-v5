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
  icons: {
    icon: "/griddy/spark.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full bg-[var(--color-page)] text-[var(--color-text)]">
        <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(900px_circle_at_8%_0%,rgba(0,0,0,0.08),transparent_45%),radial-gradient(840px_circle_at_92%_8%,rgba(0,0,0,0.06),transparent_40%),linear-gradient(180deg,#ffffff_0%,#f2f2f2_100%)]" />
        {children}
      </body>
    </html>
  );
}
