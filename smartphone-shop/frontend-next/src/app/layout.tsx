import type { Metadata } from "next";
import { DM_Mono, DM_Sans } from "next/font/google";
import { ThemeManager } from "@/components/theme-manager";
import { THEME_INIT_SCRIPT } from "@/lib/theme-init-script";
import "./globals.css";

const dmSans = DM_Sans({
  variable: "--font-dm-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const dmMono = DM_Mono({
  variable: "--font-dm-mono",
  subsets: ["latin"],
  weight: ["400", "500"],
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
    <html
      lang="en"
      data-theme="light"
      suppressHydrationWarning
      className={`${dmSans.variable} ${dmMono.variable} h-full antialiased`}
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_INIT_SCRIPT }} />
      </head>
      <body className="page-bg-grid min-h-full bg-[var(--color-page)] text-[var(--color-text)]">
        <ThemeManager />
        <div className="pointer-events-none fixed inset-0 -z-10 bg-[var(--color-page)]" />
        {children}
      </body>
    </html>
  );
}
