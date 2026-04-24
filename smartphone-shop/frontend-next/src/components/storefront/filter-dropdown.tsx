"use client";

import { ChevronDown } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

export type FilterDropdownOption = {
  label: string;
  value: string;
};

type FilterDropdownProps = {
  label?: string;
  options: FilterDropdownOption[];
  value: string;
  onChange: (value: string) => void;
  triggerClassName?: string;
  disabled?: boolean;
};

export function FilterDropdown({
  label,
  options,
  value,
  onChange,
  triggerClassName,
  disabled = false,
}: FilterDropdownProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const listHeightClass = options.length <= 10 ? "max-h-[24rem]" : "max-h-64";

  const selectedLabel = useMemo(
    () => options.find((option) => option.value === value)?.label ?? options[0]?.label ?? "",
    [options, value],
  );

  useEffect(() => {
    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current) {
        return;
      }
      if (!rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", onPointerDown);
    return () => document.removeEventListener("mousedown", onPointerDown);
  }, []);

  return (
    <div className="flex flex-col gap-2" ref={rootRef}>
      {label ? <span className="text-sm font-medium text-slate-700">{label}</span> : null}
      <div className="relative">
        <button
          type="button"
          onClick={() => setOpen((current) => !current)}
          className={`ui-input ui-dropdown-trigger flex w-full items-center justify-between px-3 py-2 text-sm text-left ${disabled ? "cursor-not-allowed opacity-65" : ""} ${triggerClassName ?? ""}`}
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-disabled={disabled}
          disabled={disabled}
          data-open={open ? "true" : "false"}
        >
          <span className="truncate">{selectedLabel}</span>
          <ChevronDown className="ui-dropdown-trigger-icon h-4 w-4 shrink-0" />
        </button>

        {open && !disabled ? (
          <div className="ui-dropdown-panel absolute left-0 right-0 top-[calc(100%+0.45rem)] z-40 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-[0_8px_18px_rgba(0,0,0,0.32)]">
            <ul role="listbox" className={`${listHeightClass} space-y-1 overflow-y-auto`}>
              {options.map((option) => {
                const active = option.value === value;
                return (
                  <li key={`${label ?? "filter"}-${option.value || "all"}`}>
                    <button
                      type="button"
                      onClick={() => {
                        onChange(option.value);
                        setOpen(false);
                      }}
                      className={[
                        "w-full rounded-lg px-3 py-2 text-left text-sm transition-[background-color,color] duration-150",
                        active
                          ? "bg-white/10 text-white"
                          : "text-[var(--color-text-muted)] hover:bg-white hover:text-black",
                      ].join(" ")}
                    >
                      {option.label}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        ) : null}
      </div>
    </div>
  );
}
