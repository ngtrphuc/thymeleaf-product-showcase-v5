import { StorefrontBrandLink } from "@/components/storefront/storefront-brand-link";
import { StorefrontChatBubble } from "@/components/storefront/storefront-chat-bubble";
import { StorefrontCompareBanner } from "@/components/storefront/storefront-compare-banner";
import { StorefrontFooter } from "@/components/storefront/storefront-footer";
import { StorefrontHeaderDockNav } from "@/components/storefront/storefront-header-dock-nav";

export default function StorefrontLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
      <header className="glass-panel mb-6 rounded-2xl px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <StorefrontBrandLink />
          <StorefrontHeaderDockNav />
        </div>
      </header>
      <main className="flex-1">{children}</main>
      <StorefrontFooter />
      <StorefrontCompareBanner />
      <StorefrontChatBubble />
    </div>
  );
}
