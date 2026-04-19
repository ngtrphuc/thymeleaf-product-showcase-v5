"use client";

import { type ReactNode, useMemo, useState } from "react";

export type VercelTabData = {
  label: string;
  value: string;
  content: ReactNode;
};

type VercelTabsProps = {
  tabs: VercelTabData[];
  defaultTab?: string;
  className?: string;
};

export function VercelTabs({ tabs, defaultTab, className }: VercelTabsProps) {
  const firstValue = tabs[0]?.value ?? "";
  const fallbackValue = defaultTab && tabs.some((tab) => tab.value === defaultTab) ? defaultTab : firstValue;
  const [activeValue, setActiveValue] = useState(fallbackValue);

  const activeTab = useMemo(
    () => tabs.find((tab) => tab.value === activeValue) ?? tabs[0],
    [activeValue, tabs],
  );

  if (!activeTab) {
    return null;
  }

  return (
    <div className={`space-y-4 ${className ?? ""}`}>
      <div className="overflow-x-auto">
        <div className="inline-flex min-w-full items-center gap-1 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-soft)] p-1">
          {tabs.map((tab) => {
            const active = tab.value === activeTab.value;
            return (
              <button
                key={tab.value}
                type="button"
                onClick={() => setActiveValue(tab.value)}
                className={[
                  "relative whitespace-nowrap rounded-lg px-4 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-[var(--color-page)] text-[var(--color-text)]"
                    : "text-[var(--color-text-muted)] hover:text-[var(--color-text)]",
                ].join(" ")}
                aria-current={active ? "page" : undefined}
              >
                {tab.label}
                {active ? (
                  <span className="absolute inset-x-2 -bottom-0.5 h-0.5 rounded-full bg-white" />
                ) : null}
              </button>
            );
          })}
        </div>
      </div>

      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 transition-colors duration-200 sm:p-5">
        {activeTab.content}
      </div>
    </div>
  );
}
