import type { Metadata } from "next";
import { DM_Mono, DM_Sans } from "next/font/google";
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
    <html lang="en" className={`${dmSans.variable} ${dmMono.variable} h-full antialiased`}>
      <body className="page-bg-grid min-h-full bg-[var(--color-page)] text-[var(--color-text)]">
        <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(900px_circle_at_10%_-10%,rgba(255,255,255,0.10),transparent_38%),radial-gradient(760px_circle_at_96%_2%,rgba(255,255,255,0.08),transparent_34%),linear-gradient(180deg,#0d0d10_0%,#060608_100%)]" />
        {children}
      </body>
    </html>
  );
}
