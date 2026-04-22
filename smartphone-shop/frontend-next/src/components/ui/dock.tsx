"use client";

import { type MouseEvent, type ReactNode, createContext, useContext, useEffect, useRef, useState } from "react";

type DockContextValue = {
  mouseX: number | null;
};

const DockContext = createContext<DockContextValue>({ mouseX: null });

type DockProps = {
  children: ReactNode;
  className?: string;
};

type DockItemProps = {
  children: ReactNode;
  onClick?: () => void;
  ariaLabel?: string;
  className?: string;
  active?: boolean;
};

type DockPartProps = {
  children: ReactNode;
  className?: string;
};

export function Dock({ children, className }: DockProps) {
  const [mouseX, setMouseX] = useState<number | null>(null);

  function onMouseMove(event: MouseEvent<HTMLDivElement>) {
    setMouseX(event.clientX);
  }

  function onMouseLeave() {
    setMouseX(null);
  }

  return (
    <DockContext.Provider value={{ mouseX }}>
      <div className={`flex h-24 items-end justify-center ${className ?? ""}`}>
        <div
          onMouseMove={onMouseMove}
          onMouseLeave={onMouseLeave}
          className="flex items-end gap-3 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 shadow-[0_10px_34px_rgba(0,0,0,0.45)]"
        >
          {children}
        </div>
      </div>
    </DockContext.Provider>
  );
}

export function DockItem({ children, onClick, ariaLabel, className, active = false }: DockItemProps) {
  const { mouseX } = useContext(DockContext);
  const itemRef = useRef<HTMLButtonElement | null>(null);
  const [lift, setLift] = useState(active ? 3 : 0);
  const [depth, setDepth] = useState(active ? 3 : 0);
  const [scale, setScale] = useState(active ? 1.03 : 1);

  useEffect(() => {
    const baseLift = active ? 3 : 0;
    const baseDepth = active ? 3 : 0;
    const baseScale = active ? 1.03 : 1;
    if (!itemRef.current || mouseX === null) {
      setLift(baseLift);
      setDepth(baseDepth);
      setScale(baseScale);
      return;
    }

    const rect = itemRef.current.getBoundingClientRect();
    const itemCenter = rect.left + rect.width / 2;
    const distance = Math.abs(mouseX - itemCenter);
    const influence = Math.max(0, 1 - distance / 120);
    setLift(baseLift + influence * 10);
    setDepth(baseDepth + influence * 10);
    setScale(baseScale + influence * 0.08);
  }, [active, mouseX]);

  return (
    <button
      ref={itemRef}
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className={[
        "group/dockitem ui-header-contrast ui-negative-hover relative flex h-11 w-11 origin-bottom items-center justify-center rounded-xl border transition-[transform,background-color,color,border-color,box-shadow] duration-200 ease-out",
        active
          ? "border-transparent bg-[var(--color-primary)] text-black"
          : "border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] hover:border-white/10 hover:bg-white hover:text-black",
        className ?? "",
      ].join(" ")}
      style={{
        transform: `translateY(${-lift}px) scale(${scale})`,
        zIndex: Math.round(depth),
        boxShadow: depth > 0 ? "0 10px 22px rgba(0,0,0,0.38)" : "none",
      }}
    >
      {children}
    </button>
  );
}

export function DockIcon({ children, className }: DockPartProps) {
  return <span className={`pointer-events-none flex h-5 w-5 items-center justify-center ${className ?? ""}`}>{children}</span>;
}

export function DockLabel({ children, className }: DockPartProps) {
  return (
    <span
      className={`pointer-events-none absolute -top-9 left-1/2 -translate-x-1/2 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] font-medium text-[var(--color-text)] opacity-0 shadow-[0_4px_18px_rgba(0,0,0,0.35)] transition-opacity duration-150 group-hover/dockitem:opacity-100 ${className ?? ""}`}
    >
      {children}
    </span>
  );
}
