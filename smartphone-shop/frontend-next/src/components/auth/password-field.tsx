"use client";

import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";

type PasswordFieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  name?: string;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
};

export function PasswordField({
  id,
  label,
  value,
  onChange,
  name,
  placeholder,
  autoComplete,
  required = false,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <label className="block space-y-1">
      <span className="text-sm text-slate-700">{label}</span>
      <div className="relative">
        <input
          id={id}
          name={name}
          type={visible ? "text" : "password"}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="ui-input w-full px-3 py-2 pr-11 text-sm"
          required={required}
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute right-1.5 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md border border-[var(--color-border)] bg-[var(--color-surface-soft)] text-[var(--color-text-muted)] transition-colors hover:border-[var(--color-border-2)] hover:text-[var(--color-text)]"
          aria-label={visible ? "Hide password" : "Show password"}
          title={visible ? "Hide password" : "Show password"}
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
    </label>
  );
}
